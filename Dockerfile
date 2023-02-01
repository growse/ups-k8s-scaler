FROM growse/musl-toolchains:x86_64-linux_10.2.1-zlib_1.2.12 as musltools

FROM ghcr.io/graalvm/native-image:ol8-java17-22.1.0 as gradle

RUN microdnf install findutils

COPY --from=musltools /musl-toolchains/x86_64-linux-musl-native/ /x86_64-linux-musl
ENV PATH="/x86_64-linux-musl/bin/:${PATH}"
COPY ups-k8s-scaler ups-k8s-scaler
RUN ups-k8s-scaler/gradlew -p ups-k8s-scaler nativeCompile --no-daemon
RUN ls -laht ups-k8s-scaler/build/native/nativeCompile

FROM alpine:latest as squisher
COPY --from=gradle /app/ups-k8s-scaler/build/native/nativeCompile/ups-k8s-scaler /ups-k8s-scaler
RUN apk add upx
RUN upx /ups-k8s-scaler

FROM bitnami/kubectl:1.25.6 as kubectl

FROM scratch
COPY --from=squisher /ups-k8s-scaler /ups-k8s-scaler
COPY --from=kubectl /opt/bitnami/kubectl/bin/kubectl /kubectl
ENV PATH=/
ENTRYPOINT [ "/ups-k8s-scaler" ]
CMD []
