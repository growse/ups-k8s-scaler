FROM python:3.10.4-alpine3.15

ARG NUT_VERSION=2.7.4-r10

HEALTHCHECK CMD upsc ups@localhost:3493 2>&1|grep -q stale && exit 1 || true

RUN echo '@testing http://dl-cdn.alpinelinux.org/alpine/edge/testing' \
  >>/etc/apk/repositories && \
  apk add --update nut@testing=${NUT_VERSION} kubectl@testing

ARG KUBERNETES_PYTHON_VERSION=23.3.0
RUN pip install kubernetes==${KUBERNETES_PYTHON_VERSION}

RUN touch /var/run/upsmon.pid

COPY ups-event.py /ups-event.py
COPY entrypoint.sh /entrypoint.sh
COPY upsmon.conf /etc/nut/upsmon.conf

CMD /entrypoint.sh
