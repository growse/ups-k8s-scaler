FROM debian:bullseye as musltools

RUN apt update && apt install -y curl unzip build-essential
RUN mkdir /musl
WORKDIR /musl
RUN curl -L -O http://more.musl.cc/10/x86_64-linux-musl/x86_64-linux-musl-native.tgz && tar -zxvf x86_64-linux-musl-native.tgz
RUN curl -L -o zlib-1.2.12.zip https://github.com/madler/zlib/archive/refs/tags/v1.2.12.zip && unzip zlib-1.2.12.zip
RUN rm *.tgz *.zip
ENV CC=/musl/x86_64-linux-musl-native/bin/gcc
RUN cd zlib-1.2.12 && ./configure --prefix=/musl/x86_64-linux-musl-native --static && make && make install
RUN rm -rf zlib-1.2.12

FROM ghcr.io/graalvm/native-image:ol8-java17-22.1.0 as gradle

COPY --from=musltools /musl /musl
ENV PATH="${PATH}:/musl/x86_64-linux-musl-native/bin/"
ADD ups-k8s-scaler ups-k8s-scaler
RUN ups-k8s-scaler/gradlew -p ups-k8s-scaler nativeCompile
RUN ls -laht ups-k8s-scaler/build/native/nativeCompile

FROM scratch
COPY --from=gradle /app/ups-k8s-scaler/build/native/nativeCompile/ups-k8s-scaler /ups-k8s-scaler
ENTRYPOINT [ "/ups-k8s-scaler" ]
