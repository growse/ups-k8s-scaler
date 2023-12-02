@file:Suppress("EXTENSION_SHADOWED_BY_MEMBER")

package com.growse.k8s.upsEventHandler.k8s

import io.kubernetes.client.custom.V1Patch
import io.kubernetes.client.openapi.ApiException
import io.kubernetes.client.openapi.apis.AppsV1Api
import io.kubernetes.client.openapi.apis.CoreV1Api
import io.kubernetes.client.openapi.apis.StorageV1Api
import io.kubernetes.client.openapi.models.V1DeploymentList
import io.kubernetes.client.openapi.models.V1PersistentVolumeClaimList
import io.kubernetes.client.openapi.models.V1Scale
import io.kubernetes.client.openapi.models.V1StatefulSetList
import io.kubernetes.client.openapi.models.V1StorageClassList

fun CoreV1Api.listPersistentVolumeClaimForAllNamespaces(
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
): Result<V1PersistentVolumeClaimList> =
    try {
      Result.success(
          this.listPersistentVolumeClaimForAllNamespaces(
              allowWatchBookmarks,
              _continue,
              fieldSelector,
              labelSelector,
              limit,
              pretty,
              resourceVersion,
              resourceVersionMatch,
              sendInitialEvents,
              timeoutSeconds,
              watch,
          ),
      )
    } catch (e: ApiException) {
      Result.failure(e)
    }

fun StorageV1Api.listStorageClass(
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
          this.listStorageClass(
              pretty,
              allowWatchBookmarks,
              _continue,
              fieldSelector,
              labelSelector,
              limit,
              resourceVersion,
              resourceVersionMatch,
              sendInitialEvents,
              timeoutSeconds,
              watch,
          ),
      )
    } catch (e: ApiException) {
      Result.failure(e)
    }

fun AppsV1Api.listStatefulSetForAllNamespaces(
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
): Result<V1StatefulSetList> =
    try {
      Result.success(
          this.listStatefulSetForAllNamespaces(
              allowWatchBookmarks,
              _continue,
              fieldSelector,
              labelSelector,
              limit,
              pretty,
              resourceVersion,
              resourceVersionMatch,
              sendInitialEvents,
              timeoutSeconds,
              watch,
          ),
      )
    } catch (e: ApiException) {
      Result.failure(e)
    }

fun AppsV1Api.listDeploymentForAllNamespaces(
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
): Result<V1DeploymentList> =
    try {
      Result.success(
          this.listDeploymentForAllNamespaces(
              allowWatchBookmarks,
              _continue,
              fieldSelector,
              labelSelector,
              limit,
              pretty,
              resourceVersion,
              resourceVersionMatch,
              sendInitialEvents,
              timeoutSeconds,
              watch,
          ),
      )
    } catch (e: ApiException) {
      Result.failure(e)
    }

fun AppsV1Api.patchNamespacedDeploymentScale(
    name: String? = null,
    namespace: String? = null,
    body: V1Patch? = null,
    dryRun: String? = null,
): Result<V1Scale> =
    try {
      Result.success(
          this.patchNamespacedDeploymentScale(
              name,
              namespace,
              body,
              null,
              dryRun,
              null,
              null,
              null,
          ),
      )
    } catch (e: ApiException) {
      Result.failure(e)
    }

fun AppsV1Api.patchNamespacedStatefulSetScale(
    name: String? = null,
    namespace: String? = null,
    body: V1Patch? = null,
    dryRun: String? = null,
): Result<V1Scale> =
    try {
      Result.success(
          this.patchNamespacedStatefulSetScale(
              name,
              namespace,
              body,
              null,
              dryRun,
              null,
              null,
              null,
          ),
      )
    } catch (e: ApiException) {
      Result.failure(e)
    }
