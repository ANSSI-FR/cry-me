#!/bin/sh

# Build the Synapse configuration file
python3 build-synapse-config.py /app/homeserver-template.yaml /app/homeserver.yaml

# Build the signature (specific to CRYME)
python3 build-signature.py

# Configure synapse
python3 -m synapse.app.homeserver       \
	--server-name $SYNAPSE_SERVER_NAME  \
	--config-path /app/homeserver.yaml  \
	--generate-config                   \
	--report-stats=no

# Launch synapse
/app/sources/synctl start /app/homeserver.yaml --no-daemonize
