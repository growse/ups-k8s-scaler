FROM alpine:3.15

# renovate: datasource=repology depName=alpine_edge/nut
ENV NUT_VERSION=2.8.0-r0
HEALTHCHECK CMD upsc ups@localhost:3493 2>&1|grep -q stale && exit 1 || true
RUN echo '@community http://dl-cdn.alpinelinux.org/alpine/edge/community' \
  >>/etc/apk/repositories && \
  apk add --update nut@community=${NUT_VERSION}

RUN touch /var/run/upsmon.pid
COPY entrypoint.sh /entrypoint.sh
COPY upsmon.conf /etc/nut/upsmon.conf
COPY ups-event-handler/build/native/nativeCompile/ups-event-handler /ups-event-handler
CMD /entrypoint.sh
