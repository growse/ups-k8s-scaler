@file:Suppress("EXTENSION_SHADOWED_BY_MEMBER")

package com.growse.k8s.upsEventHandler.k8s

import io.kubernetes.client.custom.V1Patch
import io.kubernetes.client.openapi.apis.AppsV1Api
import io.kubernetes.client.openapi.apis.CoreV1Api
import io.kubernetes.client.openapi.apis.StorageV1Api
import io.kubernetes.client.openapi.models.*

fun CoreV1Api.listPersistentVolumeClaimForAllNamespaces(
    allowWatchBookmarks: Boolean? = null,
    _continue: String? = null,
    fieldSelector: String? = null,
    labelSelector: String? = null,
    limit: Int? = null,
    pretty: String? = null,
    resourceVersion: String? = null,
    resourceVersionMatch: String? = null,
    timeoutSeconds: Int? = null,
    watch: Boolean? = null
): V1PersistentVolumeClaimList = this.listPersistentVolumeClaimForAllNamespaces(
    allowWatchBookmarks,
    _continue,
    fieldSelector,
    labelSelector,
    limit,
    pretty,
    resourceVersion,
    resourceVersionMatch,
    timeoutSeconds,
    watch
)

fun StorageV1Api.listStorageClass(
    allowWatchBookmarks: Boolean? = null,
    _continue: String? = null,
    fieldSelector: String? = null,
    labelSelector: String? = null,
    limit: Int? = null,
    pretty: String? = null,
    resourceVersion: String? = null,
    resourceVersionMatch: String? = null,
    timeoutSeconds: Int? = null,
    watch: Boolean? = null
): V1StorageClassList = this.listStorageClass(
    pretty,
    allowWatchBookmarks,
    _continue,
    fieldSelector,
    labelSelector,
    limit,
    resourceVersion,
    resourceVersionMatch,
    timeoutSeconds,
    watch
)

fun AppsV1Api.listStatefulSetForAllNamespaces(
    allowWatchBookmarks: Boolean? = null,
    _continue: String? = null,
    fieldSelector: String? = null,
    labelSelector: String? = null,
    limit: Int? = null,
    pretty: String? = null,
    resourceVersion: String? = null,
    resourceVersionMatch: String? = null,
    timeoutSeconds: Int? = null,
    watch: Boolean? = null
): V1StatefulSetList = this.listStatefulSetForAllNamespaces(
    allowWatchBookmarks,
    _continue,
    fieldSelector,
    labelSelector,
    limit,
    pretty,
    resourceVersion,
    resourceVersionMatch,
    timeoutSeconds,
    watch
)

fun AppsV1Api.listDeploymentForAllNamespaces(
    allowWatchBookmarks: Boolean? = null,
    _continue: String? = null,
    fieldSelector: String? = null,
    labelSelector: String? = null,
    limit: Int? = null,
    pretty: String? = null,
    resourceVersion: String? = null,
    resourceVersionMatch: String? = null,
    timeoutSeconds: Int? = null,
    watch: Boolean? = null
): V1DeploymentList = this.listDeploymentForAllNamespaces(
    allowWatchBookmarks,
    _continue,
    fieldSelector,
    labelSelector,
    limit,
    pretty,
    resourceVersion,
    resourceVersionMatch,
    timeoutSeconds,
    watch
)

fun AppsV1Api.patchNamespacedDeploymentScale(
    name: String? = null,
    namespace: String? = null,
    body: V1Patch? = null,
    dryRun: String? = null,
): V1Scale = this.patchNamespacedDeploymentScale(
    name, namespace, body, null, dryRun, null, null, null
)

fun AppsV1Api.patchNamespacedStatefulSetScale(
    name: String? = null,
    namespace: String? = null,
    body: V1Patch? = null,
    dryRun: String? = null,
): V1Scale = this.patchNamespacedStatefulSetScale(
    name, namespace, body, null, dryRun, null, null, null
)