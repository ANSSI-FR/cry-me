FROM alpine:3.17
WORKDIR /app/
COPY --chown=guest:users --chmod=744 ./src/synapse/run.sh ./
COPY --chown=guest:users --chmod=644 ./src/synapse/homeserver-template.yaml ./
COPY --chown=guest:users --chmod=644 ./src/synapse/server-private-key.pem ./synapse/
COPY --chown=guest:users --chmod=644 ./src/synapse/build-signature.py ./
COPY --chown=guest:users --chmod=644 ./src/synapse/build-synapse-config.py ./
COPY --chown=guest:users --chmod=644 ./src/synapse/sources/ sources/
RUN apk add --update --no-cache                  \
        python3=3.10.11-r0                       \
        python3-dev=3.10.11-r0                   \
        py3-pip=22.3.1-r1                        \
        py3-pycryptodome=3.15.0-r0               \
        gcc=12.2.1_git20220924-r4                \
        musl-dev=1.2.3-r5                        \
        libpq-dev=15.3-r0                     && \
    chmod +x ./sources/synctl                 && \
    chown guest:users /app/                   && \
    cd sources                                && \
    pip install --no-cache-dir ".[all,dev]"   && \
    pip install --no-cache-dir .              && \
    pip install --no-cache-dir ".[postgres]"
USER guest
EXPOSE 8008
CMD ["/app/run.sh"]
