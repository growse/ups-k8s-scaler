FROM alpine:3.15

# renovate: datasource=repology depName=alpine_edge/nut
ENV NUT_VERSION=2.7.4-r10
HEALTHCHECK CMD upsc ups@localhost:3493 2>&1|grep -q stale && exit 1 || true
RUN echo '@testing http://dl-cdn.alpinelinux.org/alpine/edge/testing' \
  >>/etc/apk/repositories && \
  apk add --update nut@testing=${NUT_VERSION}

RUN touch /var/run/upsmon.pid
COPY ups-event-scaler/build/native/nativeCompile/ups-event-scaler /ups-event-handler
COPY entrypoint.sh /entrypoint.sh
COPY upsmon.conf /etc/nut/upsmon.conf

CMD /entrypoint.sh
