package com.growse.k8s.upsEventHandler.k8s

import io.kubernetes.client.common.KubernetesObject
import io.kubernetes.client.custom.V1Patch
import io.kubernetes.client.openapi.ApiException
import io.kubernetes.client.openapi.apis.AppsV1Api
import io.kubernetes.client.openapi.apis.CoreV1Api
import io.kubernetes.client.openapi.apis.StorageV1Api
import io.kubernetes.client.openapi.models.V1Deployment
import io.kubernetes.client.openapi.models.V1StatefulSet
import kotlinx.coroutines.delay
import kotlinx.coroutines.yield
import mu.KotlinLogging
import kotlin.time.Duration.Companion.seconds

typealias Deployment = Either.Left<KubernetesObject, V1Deployment, V1StatefulSet>
typealias StatefulSet = Either.Right<KubernetesObject, V1Deployment, V1StatefulSet>

const val storageClassLabelSelector: String = "com.growse.k8s.nut/scale-on-battery=true"
const val orderLabelKey = "com.growse.k8s.nut/scale-order"
const val defaultOrderValue = 50
const val onlineDelayLabelKey = "com.growse.k8s.nut/online-delay"
const val defaultOnlineDelay = 1 // s

enum class ScaleDirection { UP, DOWN }

private val logger = KotlinLogging.logger {}

suspend fun scaleK8sResources(scaleDirection: ScaleDirection, dryRun: Boolean = false) {
    // names of storage classes that are maybe about to go away
    val storageClassNames = StorageV1Api()
        .listStorageClass(labelSelector = storageClassLabelSelector)
        .items
        .mapNotNull { storageClass -> storageClass.metadata?.name }

    yield()

    // all persistent volumumes using those storage classes
    val persistentVolumesWithStorageClassNames = CoreV1Api()
        .listPersistentVolumeClaimForAllNamespaces()
        .items
        .filter { storageClassNames.contains(it.spec?.storageClassName) }
        .mapNotNull { it.metadata?.name }

    yield()

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

    yield()

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

    yield()

    // Collect everything, sort in some sensible way, and then scale
    (statefulSetsWithVolumesUsingStorageClasses + deploymentsWithVolumesUsingStorageClasses)
        .sortedBy {
            // If we're going online, we need to do everything in reverse order
            when (scaleDirection) {
                ScaleDirection.UP -> -1
                ScaleDirection.DOWN -> 1
            } * (it.it().metadata.labels?.getOrDefault(orderLabelKey, null)?.toInt() ?: defaultOrderValue)
        }.map {
            when (it) {
                is StatefulSet -> ScalableThingWithScaleFunction(
                    it.it(), AppsV1Api()::patchNamespacedStatefulSetScale
                )
                is Deployment -> ScalableThingWithScaleFunction(
                    it.it(), AppsV1Api()::patchNamespacedDeploymentScale
                )
            }
        }.forEach {
            try {
                val replicas = when (scaleDirection) {
                    ScaleDirection.UP -> 1
                    ScaleDirection.DOWN -> 0
                }
                val name = "${it.obj.metadata.namespace}/${it.obj.metadata.name}"
                if (scaleDirection == ScaleDirection.UP) {
                    val delay = it.obj.metadata?.labels?.get(onlineDelayLabelKey)?.toIntOrNull() ?: defaultOnlineDelay
                    logger.info { "Pausing for $delay seconds before scaling $name up" }
                    if (!dryRun) {
                        delay(delay.seconds)
                    }
                }
                logger.info { "Scaling $name to replicas=$replicas" }
                it.scale(
                    it.obj.metadata.name,
                    it.obj.metadata.namespace,
                    V1Patch("[{\"op\": \"replace\",\"path\":\"/spec/replicas\",\"value\": $replicas}]"),
                    if (dryRun) "All" else null
                )
                yield()
            } catch (e: ApiException) {
                logger.error(e.responseBody)
            }
        }
}
