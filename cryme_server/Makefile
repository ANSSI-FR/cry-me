ifeq ($(FORCE_REBUILD),1)
NOCACHE=--no-cache
RECREATE=--force-recreate
BUILD=--build
else
NOCACHE=
RECREATE=
BUILD=
endif
ifeq ($(VERBOSE),1)
BUILDKIT_PROGRESS=plain
endif

launch_server:
	DOCKER_BUILDKIT=1 BUILDKIT_PROGRESS=$(BUILDKIT_PROGRESS) SYNAPSE_SERVER_NAME=$(SYNAPSE_SERVER_NAME) COMPOSE_DOCKER_CLI_BUILD=1 docker-compose up $(RECREATE) $(BUILD)

launch_server_build_synapse:
	DOCKER_BUILDKIT=1 BUILDKIT_PROGRESS=$(BUILDKIT_PROGRESS) SYNAPSE_SERVER_NAME=$(SYNAPSE_SERVER_NAME) COMPOSE_DOCKER_CLI_BUILD=1 docker-compose -f docker-compose.build.yml up $(RECREATE) $(BUILD)

clean_db:
	docker-compose rm cryme-db

clean_synapse:
	docker-compose rm cryme-synapse

clean_nginx:
	docker-compose rm cryme-nginx

clean: clean_db clean_synapse clean_nginx
