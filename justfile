build:
    docker buildx build -t ups-k8s-scaler .

scale-down:
    docker run -v ~/.kube/config:/KUBECONFIG -e KUBECONFIG=/KUBECONFIG --rm -it ups-k8s-scaler -H 192.168.2.2 --scale-down-immediately

scale-up:
    docker run -v ~/.kube/config:/KUBECONFIG -e KUBECONFIG=/KUBECONFIG --rm -it ups-k8s-scaler -H 192.168.2.2 --scale-up-immediately
