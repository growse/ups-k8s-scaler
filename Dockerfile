FROM scratch
COPY ups-k8s-scaler/build/native/nativeCompile/ups-k8s-scaler /ups-k8s-scaler
ENTRYPOINT [ "/ups-k8s-scaler" ]
