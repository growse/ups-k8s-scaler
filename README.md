# ups-k8s-scaler

A little k8s pod / container that can carry out k8s scaling actions when a UPS goes on battey / on line power.

I have a small k8s cluster, and it uses storage from a NAS device that exposes block devices over iSCSI. When the power goes out and the UPS battery eventually gives up, everything goes down pretty hard, which sometimes has the habit of making whoopsies all over some of the filesystems. So, this process aims to do a graceful scale-down of any k8s pod that's got a volume mounted from the NAS. That way, when everything goes lights-out, all the filesystems should already be quiesced.

## How it works

It's a single binary (and also container) that connects to a remote instance of `upsd`, and then polls the first available UPS there. If it detects that the UPS state has changed (between "OnLine", "OnBattery" and "LowBattery"), it then connects to the k8s API, finds all storage classes that are tagged with a label of `com.growse.k8s.nut/scale-on-battery=true`, then finds all deployments that have volumes with persistent volume claims using one of those storage classes, and then either scales those deployments up or down depending on the UPS status change. By default, it will scale to 1 if the new status is "OnLine", and to 0 if the status is "LowBattery".

## Running

### Environment variables

| Name                                   | Default     | Description                                                                                  |
| -------------------------------------- | ----------- | -------------------------------------------------------------------------------------------- |
| `UPS_NAME`                          | `ups`       | Name of the UPS to connect to on the target                                                  |
| `UPSD_HOSTNAME`                          | `localhost` | Hostname of running master `upsd` instance to connect to                                     |
| `UPSD_PORT`                          | `3493`      | Port of the running mast `upsd` instance to connect to                                       |
| `DEBUG_LOG`                            | `false`     | Whether or not to enable debug logging                                                       |
| `SCALE_DOWN_IMMEDIATELY_ON_POWER_LOSS` | `false`     | If enabled, start scaling down as soon as power is lost, rather than waiting for low battery |
| `DRY_RUN`                              | `false`     | If set, don't actually make any k8s scale changes, just print out what would happen          |

### Locally

```shell
Usage: ups-k8s-scaler [OPTIONS]

Options:
  --scale-down-immediately-on-power-loss
                                   Scale down immediately on power loss
  --hostname TEXT                  Hostname of the remote upsd instance to
                                   connect to
  --port INT                       Port of the remote upsd instance to connect
                                   to
  --dry-run                        Dry run scaling actions
  --debug                          Enable debug logging
  -h, --help                       Show this message and exit

```

### Kubernetes

Set the desired values for the environment in the k8s manifest, then:

```shell
$ kubectl apply -f example-k8s-manifest.yml
$
```

### Docker locally

```shell
$ docker run --rm -it -e KUBECONFIG=/kubeConfig  -e UPSD_HOSTNAME=my-hostname.example.com -v/home/growse/.kube/config:/kubeConfig ghcr.io/growse/ups-k8s-scaler:main
$
```

## TODO

- [x] Do statefulsets as well
- [x] Figure out a way of setting a dependency ordering
- [ ] Use a label to manually include deployments / statefulsets that don't explicitly use a volume.
- [ ] Use a label to exclude a statefulset / deployment that shouldn't be scaled
- [ ] Actual dependency graph
- [ ] Parallelize kuberenetes actions
- [ ] Wait for online before proceeding
