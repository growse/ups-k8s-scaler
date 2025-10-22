# syntax=docker/dockerfile:1

FROM eclipse-temurin:24-jdk-alpine as build

WORKDIR /app
COPY ups-k8s-scaler/src ups-k8s-scaler/src
COPY ups-k8s-scaler/gradle ups-k8s-scaler/gradle
COPY ups-k8s-scaler/*.kts ups-k8s-scaler/
COPY ups-k8s-scaler/gradlew ups-k8s-scaler/
RUN --mount=type=cache,id=gradle,target=/root/.gradle ups-k8s-scaler/gradlew -p ups-k8s-scaler assembleDist --no-daemon

FROM eclipse-temurin:24-jre-alpine-3.22
RUN apk add kubectl
COPY --from=build /app/ups-k8s-scaler/build/distributions/ups-k8s-scaler-*.tar /ups-k8s-scaler.tar
RUN tar xvf /ups-k8s-scaler.tar
ENTRYPOINT [ "/ups-k8s-scaler-1.0-SNAPSHOT/bin/ups-k8s-scaler" ]
CMD []
