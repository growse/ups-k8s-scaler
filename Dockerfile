# syntax=docker/dockerfile:1

FROM ghcr.io/graalvm/native-image:ol9-java17-22.3.2 as gradle

RUN microdnf -y install findutils unzip

RUN mkdir /musl-toolchains
WORKDIR /musl-toolchains

ARG MUSL_VERSION=10.2.1
ARG ZLIB_VERSION=1.2.13
RUN curl -L -O http://more.musl.cc/${MUSL_VERSION}/x86_64-linux-musl/x86_64-linux-musl-native.tgz && tar -zxvf x86_64-linux-musl-native.tgz && rm x86_64-linux-musl-native.tgz
RUN curl -L -o zlib.zip https://github.com/madler/zlib/archive/refs/tags/v${ZLIB_VERSION}.zip && unzip zlib.zip && rm zlib.zip

WORKDIR /musl-toolchains/zlib-${ZLIB_VERSION}
ENV CC=/musl-toolchains/x86_64-linux-musl-native/bin/gcc
RUN ./configure --prefix=/musl-toolchains/x86_64-linux-musl-native --static && make && make install
RUN rm -rf /musl-toolchains/zlib-*

ENV PATH="/musl-toolchains/x86_64-linux-musl-native/bin/:${PATH}"
WORKDIR /app
COPY ups-k8s-scaler ups-k8s-scaler
RUN --mount=type=cache,id=gradle,target=/root/.gradle ups-k8s-scaler/gradlew -p ups-k8s-scaler nativeCompile --no-daemon

FROM alpine:latest as squisher
COPY --from=gradle /app/ups-k8s-scaler/build/native/nativeCompile/ups-k8s-scaler /ups-k8s-scaler
RUN apk add upx
RUN upx /ups-k8s-scaler

FROM bitnami/kubectl:1.27.1 as kubectl

FROM alpine:3.18
COPY --from=gradle /app/ups-k8s-scaler/build/native/nativeCompile/ups-k8s-scaler /ups-k8s-scaler
COPY --from=kubectl /opt/bitnami/kubectl/bin/kubectl /kubectl
ENV PATH=/
ENTRYPOINT [ "/ups-k8s-scaler" ]
CMD []
