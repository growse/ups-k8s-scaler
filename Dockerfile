# syntax=docker/dockerfile:1

FROM ghcr.io/graalvm/native-image-community:23.0.0-muslib as build

RUN microdnf -y install findutils unzip

WORKDIR /app
COPY ups-k8s-scaler ups-k8s-scaler
RUN --mount=type=cache,id=gradle,target=/root/.gradle ups-k8s-scaler/gradlew -p ups-k8s-scaler nativeCompile --no-daemon

FROM alpine:latest as squisher
COPY --from=build /app/ups-k8s-scaler/build/native/nativeCompile/ups-k8s-scaler /ups-k8s-scaler
RUN apk add upx
RUN upx /ups-k8s-scaler

FROM bitnami/kubectl:1.31.1 as kubectl

FROM alpine:3.20
COPY --from=build /app/ups-k8s-scaler/build/native/nativeCompile/ups-k8s-scaler /ups-k8s-scaler
COPY --from=kubectl /opt/bitnami/kubectl/bin/kubectl /kubectl
ENV PATH=/
ENTRYPOINT [ "/ups-k8s-scaler" ]
CMD []
