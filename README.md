# ups-k8s-scaler

A little k8s pod / container that can carry out k8s scaling actions when a UPS goes on battey / on line power.

I have a small k8s cluster, and it uses storage from a NAS device that exposes block devices over iSCSI. When the power goes out and the UPS battery eventually gives up, everything goes down pretty hard, which sometimes has the habit of making whoopsies all over some of the filesystems. So, this process aims to do a graceful shutdown of any k8s pod that's got a volume mounted from the NAS. That way, when everything goes lights-out, all the filesystems should already be quiesced.

## How it works

Simply runs an instance of `nut` (https://networkupstools.org/) in slave mode connecting to a master instance that's actually managing one or more UPS's. `nut` can be configured to execute a program whenever certain events happen, so this is configured to simply listen for `ONLINE` and `ONBATT` and then execute a magic script.

The script then connects to the k8s API, finds all storage classes that are tagged with a label of `com.growse.k8s.nut/scale-on-battery=true`, then finds all deployments that have volumes with persistent volume claims using one of those storage classes, and then either scales those deployments to 0 (if going `ONBATT`) or 1 (if going `ONLINE`).

## Running

### Environment variables

| name            | default     | description                                                                         |
| --------------- | ----------- | ----------------------------------------------------------------------------------- |
| UPSMON_NAME     | `ups`       | Name of the UPS to connect to on the target                                         |
| UPSMON_HOST     | `localhost` | Hostname of running master `nut` instance to connect to                             |
| UPSMON_PORT     | `3493`      | Port of the running mast `nut` instance to connect to                               |
| UPSMON_USERNAME | `username`  | Username to use when authenticating to the remote `nut` instance                    |
| UPSMON_PASSWORD | `password`  | Password to use when authenticating to the remote `nut` instance                    |
| DRY_RUN         | `false`     | If set, don't actually make any k8s scale changes, just print out what would happen |

### Kubernetes

Set the desired values for the environment in the k8s manifest, then:

```shell
$ kubectl apply -f example-k8s-manifest.yml
```

### Docker locally

```shell
$ docker run -it -eUPSMON_HOST=my-ups-host -eUPSMON_USERNAME=upsmon -eUPSMON_PASSWORD=fixmepass ghcr.io/growse/ups-k8s-scaler:main
```

## TODO

- [ ] Do statefulsets as well
- [ ] Figure out a way of setting a dependency ordering
- [ ] Use a label to manually include deployments / statefulsets that don't explicitly use a volume.
