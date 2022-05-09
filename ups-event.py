#!/usr/bin/env python3

import fcntl
from importlib.metadata import metadata
from itertools import chain
import logging
from functools import partial
import os
import sys
from time import sleep
from typing import List, Optional, Set, Union
from kubernetes import client, config
from kubernetes.client.models.v1_stateful_set import V1StatefulSet
from kubernetes.client.models.v1_deployment import V1Deployment
from kubernetes.client.models.v1_storage_class import V1StorageClass
from kubernetes.client.models.v1_persistent_volume_claim import V1PersistentVolumeClaim

STORAGE_CLASS_LABEL = "com.growse.k8s.nut/scale-on-battery=true"
ORDER_LABEL = "com.growse.k8s.nut/scale-order"
ONLINE_DELAY_LABEL = "com.growse.k8s.nut/online-delay"
DEFAULT_ORDERING = 50
DEFAULT_DELAY = 1  # s

logging.basicConfig(level=logging.INFO)


def singleton_guard():
    logging.debug("trying to get singleton lock")
    lock_filename = f"/tmp/ups-event-k8s-scaler.lock"
    global lock_file_pointer
    lock_file_pointer = os.open(lock_filename, os.O_WRONLY | os.O_CREAT)
    locked = False
    while not locked:
        try:
            fcntl.lockf(lock_file_pointer, fcntl.LOCK_EX | fcntl.LOCK_NB)
            locked = True
            logging.debug(f"Acquired singleton lock at {lock_filename}")
        except IOError:
            logging.warning("Another instance running")
            locked = False
            sleep(2)


def deployment_has_replicas(deployment) -> bool:
    return deployment.status.replicas > 0


def get_deployments() -> List[V1Deployment]:
    return client.AppsV1Api().list_deployment_for_all_namespaces().items


def get_stateful_sets() -> List[V1StatefulSet]:
    return client.AppsV1Api().list_stateful_set_for_all_namespaces().items


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


def filter_deployment_has_scale_order_labels(deployment: V1Deployment) -> bool:
    return (
        deployment.metadata is not None
        and deployment.metadata.labels is not None
        and ORDER_LABEL in deployment.metadata.labels
    )


def filter_stateful_set_has_volumes_with_storage_class(
    storage_classes: List[V1StorageClass], stateful_set: V1StatefulSet
) -> bool:
    storage_class_names: Set[str] = {
        x for x in map(storage_class_name, storage_classes) if x is not None
    }
    return (
        stateful_set.spec is not None
        and stateful_set.spec.volume_claim_templates is not None
        and any(
            map(
                lambda volume_claim_template: volume_claim_template is not None
                and volume_claim_template.spec is not None
                and volume_claim_template.spec.storage_class_name
                in storage_class_names,
                stateful_set.spec.volume_claim_templates,
            )
        )
    )


def filter_deployment_has_volumes_with_persistent_volume_claims(
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


def scale_deployment(
    k8s_object: Union[V1Deployment, V1StatefulSet], replicas: int, dry_run: bool
):
    if (
        k8s_object.metadata is not None
        and k8s_object.metadata.name is not None
        and k8s_object.metadata.namespace is not None
    ):
        logging.info(
            f"Scaling {k8s_object.metadata.name} in {k8s_object.metadata.namespace} to replicas={replicas}"
        )
        if not dry_run:
            if k8s_object is V1Deployment:
                client.AppsV1Api().patch_namespaced_deployment_scale(
                    k8s_object.metadata.name,
                    k8s_object.metadata.namespace,
                    {"spec": {"replicas": replicas}},
                )
            elif k8s_object is V1StatefulSet:
                client.AppsV1Api().patch_namespaced_stateful_set_scale(
                    k8s_object.metadata.name,
                    k8s_object.metadata.namespace,
                    {"spec": {"replicas": replicas}},
                )


def scale_order(invert: bool, k8s_object: Union[V1StatefulSet, V1Deployment]) -> int:
    invert_factor = 1 if invert else -1
    if k8s_object.metadata is None or k8s_object.metadata.labels is None:
        return invert_factor * DEFAULT_ORDERING
    else:
        try:
            return invert_factor * int(
                k8s_object.metadata.labels.get(ORDER_LABEL, DEFAULT_ORDERING)
            )
        except ValueError:
            logging.warning(
                f"Unable to convert ordering {k8s_object.metadata.labels.get(ORDER_LABEL, DEFAULT_ORDERING)} to an integer"
            )
            return invert_factor * DEFAULT_ORDERING


def get_online_delay(k8s_object: Union[V1StatefulSet, V1Deployment]) -> int:
    if k8s_object.metadata is None or k8s_object.metadata.labels is None:
        return DEFAULT_DELAY
    else:
        try:
            return int(
                k8s_object.metadata.labels.get(ONLINE_DELAY_LABEL, DEFAULT_DELAY)
            )
        except ValueError:
            logging.warning(
                f"Unable to convert delay {k8s_object.metadata.labels.get(ONLINE_DELAY_LABEL, DEFAULT_DELAY)} to an integer"
            )
            return DEFAULT_DELAY


if __name__ == "__main__":
    power_lost_event = "lowbatt"
    if os.environ.get("SCALE_IMMEDIATELY_ON_POWER_LOSS", False):
        power_lost_event = "onbatt"
    if len(sys.argv) == 2:
        match sys.argv[1]:
            case "online":
                logging.info("Scaling up")
                replicas = 1
            case "lowbatt" | "onbatt":
                if power_lost_event == sys.argv[1]:
                    logging.info("Scaling down")
                    replicas = 0
                else:
                    logging.warning(f"Ignoring UPS event {sys.argv[1]}")
                    exit(1)
            case _:
                logging.error(f"Unknown UPS event {sys.argv[1]}")
                exit(1)
    else:
        logging.error("No UPS event provided")
        exit(1)

    dry_run = False
    if os.environ.get("DRY_RUN", False):
        dry_run = True
        logging.warning("Dry run mode. Not actually doing anything")

    singleton_guard()

    config.load_config()

    persistent_volume_claims = list(
        filter(
            partial(
                filter_persistent_volume_claim_has_storage_class, get_storage_classes()
            ),
            get_persistent_volume_claims(),
        )
    )

    stateful_sets = filter(
        partial(
            filter_stateful_set_has_volumes_with_storage_class,
            get_storage_classes(),
        ),
        get_stateful_sets(),
    )
    deployments = filter(
        lambda deployment: filter_deployment_has_scale_order_labels(deployment)
        or filter_deployment_has_volumes_with_persistent_volume_claims(
            persistent_volume_claims, deployment
        ),
        get_deployments(),
    )

    for deployment_or_stateful_set in sorted(
        chain(deployments, stateful_sets),
        key=partial(scale_order, replicas == 0),
    ):
        if replicas != 0:
            delay = get_online_delay(deployment_or_stateful_set)
            logging.info(
                f"Pausing for {delay} seconds before bringing {deployment_or_stateful_set.metadata.name} online"
            )
            if not dry_run:
                sleep(delay)
        scale_deployment(deployment_or_stateful_set, replicas, dry_run is not None)

    # for deployment in sorted(
    # filter(
    #     lambda deployment: filter_deployment_has_scale_order_labels(deployment)
    #     or filter_deployment_has_volumes_with_persistent_volume_claims(
    #         persistent_volume_claims, deployment
    #     ),
    #     get_deployments(),
    # ),
    #     key=partial(scale_order, replicas == 0),
    # ):
    #     scale_deployment(deployment, replicas, dry_run is not None)
    #     if not dry_run:
    #         sleep(DELAY_BETWEEN_EVENTS)
