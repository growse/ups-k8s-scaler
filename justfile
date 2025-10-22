set positional-arguments := true
set dotenv-load := true

gradlec := "./ups-k8s-scaler/gradlew -p ups-k8s-scaler/"

default:
    @just --list

tasks:
    {{gradlec}} tasks

build:
    {{gradlec}} assembleDist --scan

test:
    {{gradlec}} build --scan

coverage:
    {{gradlec}} test jacocoTestReport
    @echo "Coverage report: ups-k8s-scaler/build/reports/jacoco/index.html"

clean:
    {{gradlec}} clean

format:
    {{gradlec}} ktfmtFormat

build-container:
    docker buildx build -t ups-k8s-scaler .

scale-down:
    docker run -v ~/.kube/config:/KUBECONFIG -e KUBECONFIG=/KUBECONFIG --rm -it ups-k8s-scaler --scale-down-immediately

scale-up:
    docker run -v ~/.kube/config:/KUBECONFIG -e KUBECONFIG=/KUBECONFIG --rm -it ups-k8s-scaler --scale-up-immediately

shell:
    docker run -v ~/.kube/config:/KUBECONFIG -e KUBECONFIG=/KUBECONFIG --rm -it ups-k8s-scaler /bin/bash
