import io.kubernetes.client.common.KubernetesObject
import io.kubernetes.client.custom.V1Patch
import io.kubernetes.client.openapi.ApiException
import io.kubernetes.client.openapi.Configuration
import io.kubernetes.client.openapi.apis.AppsV1Api
import io.kubernetes.client.openapi.apis.CoreV1Api
import io.kubernetes.client.openapi.apis.StorageV1Api
import io.kubernetes.client.openapi.models.V1Deployment
import io.kubernetes.client.openapi.models.V1StatefulSet
import io.kubernetes.client.util.Config
import kotlin.system.exitProcess

typealias Deployment = Either.Left<KubernetesObject, V1Deployment, V1StatefulSet>
typealias StatefulSet = Either.Right<KubernetesObject, V1Deployment, V1StatefulSet>

const val storageClassLabelSelector: String = "com.growse.k8s.nut/scale-on-battery=true"
const val orderLabelKey = "com.growse.k8s.nut/scale-order"
const val defaultOrderValue = 50

fun main(args: Array<String>) {
    val mode = getModeFromArguments(args)
    // If we're going online, we need to do everything in reverse order
    val ordering = when (mode) {
        UPSEvents.OnLine -> -1
        else -> 1
    }
    val dryRun = System.getenv().containsKey("DRY_RUN")
    if (dryRun) println("Dry run mode")
    Configuration.setDefaultApiClient(Config.defaultClient())

    // names of storage classes that are maybe about to go away
    val storageClassNames = StorageV1Api()
        .listStorageClass(labelSelector = storageClassLabelSelector)
        .items
        .mapNotNull { storageClass -> storageClass.metadata?.name }

    // all persistent volumumes using those storage classes
    val persistentVolumesWithStorageClassNames = CoreV1Api()
        .listPersistentVolumeClaimForAllNamespaces()
        .items
        .filter { storageClassNames.contains(it.spec?.storageClassName) }
        .mapNotNull { it.metadata?.name }

    // all statefulsets with volume claims using those storage classes
    val statefulSetsWithVolumesUsingStorageClasses = AppsV1Api()
        .listStatefulSetForAllNamespaces()
        .items
        .filter {
            it.spec?.volumeClaimTemplates?.any { v1PersistentVolumeClaim ->
                storageClassNames.contains(v1PersistentVolumeClaim.spec?.storageClassName)
            } ?: false
        }
        .map { StatefulSet(it) }

    // all deployments using volumes that use those storage classes
    val deploymentsWithVolumesUsingStorageClasses = AppsV1Api()
        .listDeploymentForAllNamespaces()
        .items
        .filter { deployment ->
            deployment.metadata?.labels?.containsKey(orderLabelKey) ?: false ||
                    deployment.spec?.template?.spec?.volumes?.any {
                        persistentVolumesWithStorageClassNames.contains(it.persistentVolumeClaim?.claimName)
                    } ?: false
        }.map { Deployment(it) }

    // Collect everything, sort in some sensible way, and then scale
    (statefulSetsWithVolumesUsingStorageClasses + deploymentsWithVolumesUsingStorageClasses)
        .sortedBy {
            ordering * (it.it().metadata.labels?.getOrDefault(orderLabelKey, null)?.toInt() ?: defaultOrderValue)
        }
        .map {
            when (it) {
                is StatefulSet -> ScalableThingWithScaleFunction(
                    it.it(),
                    AppsV1Api()::patchNamespacedStatefulSetScaleShort
                )
                is Deployment -> ScalableThingWithScaleFunction(
                    it.it(),
                    AppsV1Api()::patchNamespacedDeploymentScaleShort
                )
            }
        }
        .forEach {
            try {
                val replicas = when (mode) {
                    UPSEvents.OnLine -> 1
                    UPSEvents.OnBattery -> 0
                    UPSEvents.LowBattery -> 0
                }
                println("Scaling ${it.obj.metadata.namespace}/${it.obj.metadata.name} to replicas=$replicas")
                it.scale(
                    it.obj.metadata.name,
                    it.obj.metadata.namespace,
                    V1Patch("[{\"op\": \"replace\",\"path\":\"/spec/replicas\",\"value\": $replicas}]"),
                    if (dryRun) "All" else null
                )
            } catch (e: ApiException) {
                println(e.responseBody)
            }
        }
}

private fun getModeFromArguments(args: Array<String>): UPSEvents {
    if (args.isEmpty()) {
        println("No UPS event argument passed")
        exitProcess(1)
    }
    val event = UPSEvents.parse(args[0])
    if (event == null) {
        println("Unknown UPS event argument passed: ${args[0]}")
        exitProcess(1)
    }
    return event
}