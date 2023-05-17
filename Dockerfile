# syntax=docker/dockerfile:1

FROM growse/musl-toolchains:x86_64-linux_10.2.1-zlib_1.2.12 as musltools

FROM ghcr.io/graalvm/native-image:ol9-java17-22.3.2 as gradle

RUN microdnf -y install findutils

COPY --from=musltools /musl-toolchains/x86_64-linux-musl-native/ /x86_64-linux-musl
ENV PATH="/x86_64-linux-musl/bin/:${PATH}"
COPY ups-k8s-scaler ups-k8s-scaler
RUN --mount=type=cache,id=gradle,target=/root/.gradle ups-k8s-scaler/gradlew -p ups-k8s-scaler nativeCompile --no-daemon
RUN ls -laht ups-k8s-scaler/build/native/nativeCompile

FROM alpine:latest as squisher
COPY --from=gradle /app/ups-k8s-scaler/build/native/nativeCompile/ups-k8s-scaler /ups-k8s-scaler
RUN apk add upx
RUN upx /ups-k8s-scaler

FROM bitnami/kubectl:1.27.1 as kubectl

FROM scratch
COPY --from=squisher /ups-k8s-scaler /ups-k8s-scaler
COPY --from=kubectl /opt/bitnami/kubectl/bin/kubectl /kubectl
ENV PATH=/
ENTRYPOINT [ "/ups-k8s-scaler" ]
CMD []
