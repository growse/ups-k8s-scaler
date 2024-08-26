/** Let's wrap the K8S API in a more friendly way that uses functional Result types */
package com.growse.k8s.upsEventHandler.k8s

import io.kubernetes.client.openapi.ApiException
import io.kubernetes.client.openapi.apis.AppsV1Api
import io.kubernetes.client.openapi.apis.CoreV1Api
import io.kubernetes.client.openapi.apis.StorageV1Api
import io.kubernetes.client.openapi.models.V1PersistentVolumeClaimList
import io.kubernetes.client.openapi.models.V1StorageClassList

fun CoreV1Api.maybeListPersistentVolumeClaimForAllNamespaces():
    Result<V1PersistentVolumeClaimList> =
    try {
      Result.success(this.listPersistentVolumeClaimForAllNamespaces().execute())
    } catch (e: ApiException) {
      Result.failure(e)
    }

fun StorageV1Api.maybeListStorageClass(
    allowWatchBookmarks: Boolean? = null,
    _continue: String? = null,
    fieldSelector: String? = null,
    labelSelector: String? = null,
    limit: Int? = null,
    pretty: String? = null,
    resourceVersion: String? = null,
    resourceVersionMatch: String? = null,
    sendInitialEvents: Boolean? = null,
    timeoutSeconds: Int? = null,
    watch: Boolean? = null,
): Result<V1StorageClassList> =
    try {
      Result.success(
          this.listStorageClass()
              .allowWatchBookmarks(allowWatchBookmarks)
              ._continue(_continue)
              .fieldSelector(fieldSelector)
              .labelSelector(labelSelector)
              .limit(limit)
              .pretty(pretty)
              .resourceVersion(resourceVersion)
              .resourceVersionMatch(resourceVersionMatch)
              .sendInitialEvents(sendInitialEvents)
              .timeoutSeconds(timeoutSeconds)
              .watch(watch)
              .execute())
    } catch (e: ApiException) {
      Result.failure(e)
    }

fun AppsV1Api.maybeListStatefulSetForAllNamespaces() =
    try {
      Result.success(this.listStatefulSetForAllNamespaces().execute())
    } catch (e: ApiException) {
      Result.failure(e)
    }

fun AppsV1Api.maybeListDeploymentForAllNamespaces() =
    try {
      Result.success(this.listDeploymentForAllNamespaces().execute())
    } catch (e: ApiException) {
      Result.failure(e)
    }
