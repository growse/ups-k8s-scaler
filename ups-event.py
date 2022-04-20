#!/usr/bin/env python3

import fcntl
from functools import partial
import os
import sys
from time import sleep
from typing import List, Optional, Set
from kubernetes import client, config
from kubernetes.client.models.v1_deployment import V1Deployment
from kubernetes.client.models.v1_storage_class import V1StorageClass
from kubernetes.client.models.v1_volume import V1Volume
from kubernetes.client.models.v1_persistent_volume_claim import V1PersistentVolumeClaim

STORAGE_CLASS_LABEL = "com.growse.k8s.nut/scale-on-battery=true"


def singleton_guard():
    print("trying to get singleton lock")
    lock_filename = f"/tmp/ups-event-k8s-scaler.lock"
    global lock_file_pointer
    lock_file_pointer = os.open(lock_filename, os.O_WRONLY | os.O_CREAT)
    locked = False
    while not locked:
        try:
            fcntl.lockf(lock_file_pointer, fcntl.LOCK_EX | fcntl.LOCK_NB)
            locked = True
            print(f"Acquired singleton lock at {lock_filename}")
        except IOError:
            print("Another instance running")
            locked = False
            sleep(2)


def deployment_has_replicas(deployment) -> bool:
    return deployment.status.replicas > 0


def get_deployments() -> List[V1Deployment]:
    return client.AppsV1Api().list_deployment_for_all_namespaces().items


def get_storage_classes() -> List[V1StorageClass]:
    return (
        client.StorageV1Api()
        .list_storage_class(label_selector=STORAGE_CLASS_LABEL)
        .items
    )


def filter_persistent_volume_claim_has_storage_class(
    storage_classes: List[V1StorageClass],
    persistent_volume_claim: V1PersistentVolumeClaim,
):
    storage_class_names: Set[str] = {
        x for x in map(storage_class_name, storage_classes) if x is not None
    }
    return (
        persistent_volume_claim.spec is not None
        and persistent_volume_claim.spec.storage_class_name in storage_class_names
    )


def get_persistent_volume_claims() -> List[V1PersistentVolumeClaim]:
    return client.CoreV1Api().list_persistent_volume_claim_for_all_namespaces().items


def deployment_has_volumes_with_persistent_volume_claims(
    persistent_volume_claims: List[V1PersistentVolumeClaim], deployment: V1Deployment
) -> bool:
    persistent_volume_claim_names = {
        pvc.metadata.name
        for pvc in persistent_volume_claims
        if pvc.metadata is not None
    }
    return (
        deployment.spec is not None
        and deployment.spec.template.spec is not None
        and deployment.spec.template.spec.volumes is not None
        and any(
            map(
                lambda volume: volume.persistent_volume_claim is not None
                and volume.persistent_volume_claim.claim_name
                in persistent_volume_claim_names,
                deployment.spec.template.spec.volumes,
            )
        )
    )


def storage_class_name(storage_class: V1StorageClass) -> Optional[str]:
    if storage_class.metadata is not None:
        return storage_class.metadata.name
    return None


def scale_deployment(deployment: V1Deployment, replicas: int):
    if (
        deployment.metadata is not None
        and deployment.metadata.name is not None
        and deployment.metadata.namespace is not None
    ):
        print(
            f"Scaling {deployment.metadata.name} in {deployment.metadata.namespace} to replicas={replicas}"
        )
        client.AppsV1Api().patch_namespaced_deployment_scale(
            deployment.metadata.name,
            deployment.metadata.namespace,
            {"spec": {"replicas": replicas}},
        )


if __name__ == "__main__":
    if len(sys.argv) == 2:
        match sys.argv[1]:
            case "online":
                print("UPS on line power")
                replicas = 1
            case "onbatt":
                print("UPS on battery")
                replicas = 0
            case _:
                print(f"Unknown UPS event {sys.argv[1]}")
                exit(1)
    else:
        print("No UPS event provided")
        exit(1)

    singleton_guard()

    config.load_config()

    storage_classes = get_storage_classes()

    persistent_volume_claims = list(
        filter(
            partial(filter_persistent_volume_claim_has_storage_class, storage_classes),
            get_persistent_volume_claims(),
        )
    )

for deployment in list(
    filter(
        partial(
            deployment_has_volumes_with_persistent_volume_claims,
            persistent_volume_claims,
        ),
        get_deployments(),
    )
):
    scale_deployment(deployment, replicas)
