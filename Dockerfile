FROM python:alpine3.15 as base

# python
ENV PYTHONUNBUFFERED=1 \
  # prevents python creating .pyc files in the dist
  PYTHONPYCACHEPREFIX=/tmp/pycache \
  # pip
  PIP_NO_CACHE_DIR=off \
  PIP_DISABLE_PIP_VERSION_CHECK=on \
  PIP_DEFAULT_TIMEOUT=100 \
  # make poetry install to this location
  POETRY_HOME="/opt/poetry" \
  # make poetry create the virtual environment in the project's root
  # it gets named `.venv`
  POETRY_VIRTUALENVS_IN_PROJECT=true \
  # do not ask any interactive question
  POETRY_NO_INTERACTION=1 \
  # paths
  # this is where our requirements + virtual environment will live
  PYSETUP_PATH="/opt/pysetup" \
  VENV_PATH="/opt/pysetup/.venv"

ENV PATH="$POETRY_HOME/bin:$VENV_PATH/bin:$PATH"

FROM base as builder

RUN apk add --update curl gcc python3-dev musl-dev libffi-dev

# poetry
# https://python-poetry.org/docs/configuration/#using-environment-variables
# renovate: datasource=pypi depName=poetry
ENV POETRY_VERSION=1.1.13

# install poetry - respects $POETRY_VERSION & $POETRY_HOME
RUN curl -sSL https://install.python-poetry.org | python3 -

# copy project requirement files here to ensure they will be cached.
WORKDIR $PYSETUP_PATH
COPY poetry.lock pyproject.toml ./

# install runtime deps - uses $POETRY_VIRTUALENVS_IN_PROJECT internally
RUN poetry install --no-dev

FROM base

# renovate: datasource=repology depName=nut
ENV NUT_VERSION=2.7.4-r10
HEALTHCHECK CMD upsc ups@localhost:3493 2>&1|grep -q stale && exit 1 || true
RUN echo '@testing http://dl-cdn.alpinelinux.org/alpine/edge/testing' \
  >>/etc/apk/repositories && \
  apk add --update nut@testing=${NUT_VERSION}

RUN touch /var/run/upsmon.pid
COPY ups-event.py /ups-event.py
COPY --from=builder /opt/pysetup /opt/pysetup
COPY entrypoint.sh /entrypoint.sh
COPY upsmon.conf /etc/nut/upsmon.conf

CMD /entrypoint.sh
