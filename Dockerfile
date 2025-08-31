# syntax=docker/dockerfile:1

FROM ghcr.io/graalvm/native-image-community:24.0.2-muslib as build

RUN microdnf -y install findutils unzip

WORKDIR /app
COPY ups-k8s-scaler/src ups-k8s-scaler/src
COPY ups-k8s-scaler/gradle ups-k8s-scaler/gradle
COPY ups-k8s-scaler/*.kts ups-k8s-scaler/
COPY ups-k8s-scaler/gradlew ups-k8s-scaler/
RUN --mount=type=cache,id=gradle,target=/root/.gradle ups-k8s-scaler/gradlew -p ups-k8s-scaler nativeCompile --no-daemon

FROM alpine:latest as squisher
COPY --from=build /app/ups-k8s-scaler/build/native/nativeCompile/ups-k8s-scaler /ups-k8s-scaler
RUN apk add upx
RUN upx /ups-k8s-scaler

FROM rancher/kubectl:1.34.0 as kubectl

FROM alpine:3.22
COPY --from=build /app/ups-k8s-scaler/build/native/nativeCompile/ups-k8s-scaler /ups-k8s-scaler
RUN apk add kubectl
ENV PATH=/
ENTRYPOINT [ "/ups-k8s-scaler" ]
CMD []
