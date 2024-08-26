package com.growse.k8s.upsEventHandler.k8s

import io.kubernetes.client.common.KubernetesObject
import io.kubernetes.client.openapi.ApiException
import io.kubernetes.client.openapi.apis.AppsV1Api
import io.kubernetes.client.openapi.apis.CoreV1Api
import io.kubernetes.client.openapi.apis.StorageV1Api
import io.kubernetes.client.openapi.models.V1Deployment
import io.kubernetes.client.openapi.models.V1Scale
import io.kubernetes.client.openapi.models.V1ScaleSpec
import io.kubernetes.client.openapi.models.V1StatefulSet
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.delay
import kotlinx.coroutines.yield
import mu.KotlinLogging

typealias Deployment = Either.Left<KubernetesObject, V1Deployment, V1StatefulSet>

typealias StatefulSet = Either.Right<KubernetesObject, V1Deployment, V1StatefulSet>

const val STORAGE_CLASS_LABEL_SELECTOR: String = "com.growse.k8s.nut/scale-on-battery=true"
const val ORDER_LABEL_KEY = "com.growse.k8s.nut/scale-order"
const val DEFAULT_ORDER_VALUE = 50
const val ONLINE_DELAY_LABEL_KEY = "com.growse.k8s.nut/online-delay"
const val DEFAULT_ONLINE_DELAY = 1 // s

enum class ScaleDirection {
  UP,
  DOWN
}

private val logger = KotlinLogging.logger {}

fun checkK8sConnectivity(): Result<Unit> =
    StorageV1Api().maybeListStorageClass(labelSelector = STORAGE_CLASS_LABEL_SELECTOR).map {}

suspend fun scaleK8sResources(
    scaleDirection: ScaleDirection,
    dryRun: Boolean = false,
) {
  // names of storage classes that are maybe about to go away

  val storageClassNames =
      StorageV1Api()
          .maybeListStorageClass(labelSelector = STORAGE_CLASS_LABEL_SELECTOR)
          .getOrElse {
            logger.error(it) { "Unable to list StorageClassNames" }
            return
          }
          .items
          .mapNotNull { storageClass -> storageClass.metadata?.name }

  yield()

  // all persistent volumes using those storage classes
  val persistentVolumesWithStorageClassNames =
      CoreV1Api()
          .maybeListPersistentVolumeClaimForAllNamespaces()
          .getOrElse {
            logger.error(it) { "Unable to list PersistentVolumeClaims" }
            return
          }
          .items
          .filter { storageClassNames.contains(it.spec?.storageClassName) }
          .mapNotNull { it.metadata?.name }

  yield()

  // all StatefulSets with volume claims using those storage classes
  val statefulSetsWithVolumesUsingStorageClasses =
      AppsV1Api()
          .maybeListStatefulSetForAllNamespaces()
          .getOrElse {
            logger.error(it) { "Unable to list StatefulSets" }
            return
          }
          .items
          .filter {
            it.spec?.volumeClaimTemplates?.any { v1PersistentVolumeClaim ->
              storageClassNames.contains(v1PersistentVolumeClaim.spec?.storageClassName)
            } ?: false
          }
          .map { StatefulSet(it) }

  yield()

  // all deployments using volumes that use those storage classes
  val deploymentsWithVolumesUsingStorageClasses =
      AppsV1Api()
          .maybeListDeploymentForAllNamespaces()
          .getOrElse {
            logger.error(it) { "Unable to list Deployments" }
            return
          }
          .items
          .filter { deployment ->
            deployment.metadata?.labels?.containsKey(ORDER_LABEL_KEY) ?: false ||
                deployment.spec?.template?.spec?.volumes?.any {
                  persistentVolumesWithStorageClassNames.contains(
                      it.persistentVolumeClaim?.claimName)
                } ?: false
          }
          .map { Deployment(it) }

  yield()

  // Collect everything, sort in some sensible way, and then scale
  (statefulSetsWithVolumesUsingStorageClasses + deploymentsWithVolumesUsingStorageClasses)
      .sortedBy {
        // If we're going online, we need to do everything in reverse order
        when (scaleDirection) {
          ScaleDirection.UP -> -1
          ScaleDirection.DOWN -> 1
        } *
            (it.whichever().metadata.labels?.getOrDefault(ORDER_LABEL_KEY, null)?.toInt()
                ?: DEFAULT_ORDER_VALUE)
      }
      .map {
        val desiredReplicas =
            when (scaleDirection) {
              ScaleDirection.UP -> 1
              ScaleDirection.DOWN -> 0
            }
        when (it) {
          is StatefulSet ->
              ScalableThingWithScaleFunction(
                  it,
                  it.right.status?.replicas,
                  desiredReplicas,
              )
          is Deployment ->
              ScalableThingWithScaleFunction(
                  it,
                  it.left.status?.replicas,
                  desiredReplicas,
              )
        }
      }
      .filter { it.desiredReplicas != it.currentReplicas }
      .forEach {
        try {
          val name =
              "${it.thing.whichever().metadata.namespace}/${it.thing.whichever().metadata.name}"
          if (scaleDirection == ScaleDirection.UP) {
            val delay =
                it.thing.whichever().metadata?.labels?.get(ONLINE_DELAY_LABEL_KEY)?.toIntOrNull()
                    ?: DEFAULT_ONLINE_DELAY
            logger.info { "Pausing for $delay seconds before scaling $name up" }
            if (!dryRun) {
              delay(delay.seconds)
            }
          }
          logger.info {
            "Scaling $name from ${it.currentReplicas ?: "unknown"} replicas=${it.desiredReplicas}"
          }
          when (val either = it.thing) {
            is Either.Left<*, V1Deployment, *> -> {

              AppsV1Api()
                  .replaceNamespacedDeploymentScale(
                      either.left.metadata?.name,
                      either.left.metadata?.namespace,
                      V1Scale().apply {
                        spec = V1ScaleSpec().replicas(it.desiredReplicas)
                        metadata = either.left.metadata
                      })
                  .dryRun(if (dryRun) "All" else null)
                  .execute()
            }
            is Either.Right<*, *, V1StatefulSet> -> {
              AppsV1Api()
                  .replaceNamespacedStatefulSetScale(
                      either.right.metadata?.name,
                      either.right.metadata?.namespace,
                      V1Scale().apply {
                        spec = V1ScaleSpec().replicas(it.desiredReplicas)
                        metadata = either.whichever().metadata
                      })
                  .dryRun(if (dryRun) "All" else null)
                  .execute()
            }
          }
          yield()
        } catch (e: ApiException) {
          logger.error(e.responseBody)
        }
      }
}
