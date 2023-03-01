package com.growse.k8s.upsEventHandler.k8s

import io.kubernetes.client.common.KubernetesObject
import io.kubernetes.client.openapi.models.V1Deployment
import io.kubernetes.client.openapi.models.V1StatefulSet

data class ScalableThingWithScaleFunction(
    val thing: Either<KubernetesObject, V1Deployment, V1StatefulSet>,
    val currentReplicas: Int?,
    val desiredReplicas: Int,
)
