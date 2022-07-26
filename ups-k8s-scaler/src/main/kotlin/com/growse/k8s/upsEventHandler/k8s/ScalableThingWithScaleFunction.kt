package com.growse.k8s.upsEventHandler.k8s

import io.kubernetes.client.common.KubernetesObject
import io.kubernetes.client.custom.V1Patch
import io.kubernetes.client.openapi.models.V1Scale
import kotlin.reflect.KFunction4

data class ScalableThingWithScaleFunction(
    val obj: KubernetesObject,
    val scale: KFunction4<String?, String?, V1Patch?, String?, V1Scale>,
    val currentReplicas: Int?,
    val desiredReplicas: Int
)
