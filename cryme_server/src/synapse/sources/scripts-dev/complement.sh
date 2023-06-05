#!/usr/bin/env bash
# This script is designed for developers who want to test their code
# against Complement.
#
# It makes a Synapse image which represents the current checkout,
# builds a synapse-complement image on top, then runs tests with it.
#
# By default the script will fetch the latest Complement master branch and
# run tests with that. This can be overridden to use a custom Complement
# checkout by setting the COMPLEMENT_DIR environment variable to the
# filepath of a local Complement checkout.
#
# By default Synapse is run in monolith mode. This can be overridden by
# setting the WORKERS environment variable.
#
# A regular expression of test method names can be supplied as the first
# argument to the script. Complement will then only run those tests. If
# no regex is supplied, all tests are run. For example;
#
# ./complement.sh "TestOutboundFederation(Profile|Send)"
#

# Exit if a line returns a non-zero exit code
set -e

# Change to the repository root
cd "$(dirname $0)/.."

# Check for a user-specified Complement checkout
if [[ -z "$COMPLEMENT_DIR" ]]; then
  echo "COMPLEMENT_DIR not set. Fetching the latest Complement checkout..."
  wget -Nq https://github.com/matrix-org/complement/archive/master.tar.gz
  tar -xzf master.tar.gz
  COMPLEMENT_DIR=complement-master
  echo "Checkout available at 'complement-master'"
fi

# Build the base Synapse image from the local checkout
docker build -t matrixdotorg/synapse -f "docker/Dockerfile" .

# If we're using workers, modify the docker files slightly.
if [[ -n "$WORKERS" ]]; then
  # Build the workers docker image (from the base Synapse image).
  docker build -t matrixdotorg/synapse-workers -f "docker/Dockerfile-workers" .

  export COMPLEMENT_BASE_IMAGE=complement-synapse-workers
  COMPLEMENT_DOCKERFILE=SynapseWorkers.Dockerfile
  # And provide some more configuration to complement.
  export COMPLEMENT_CA=true
  export COMPLEMENT_VERSION_CHECK_ITERATIONS=500
else
  export COMPLEMENT_BASE_IMAGE=complement-synapse
  COMPLEMENT_DOCKERFILE=Synapse.Dockerfile
fi

# Build the Complement image from the Synapse image we just built.
docker build -t $COMPLEMENT_BASE_IMAGE -f "$COMPLEMENT_DIR/dockerfiles/$COMPLEMENT_DOCKERFILE" "$COMPLEMENT_DIR/dockerfiles"

cd "$COMPLEMENT_DIR"

EXTRA_COMPLEMENT_ARGS=""
if [[ -n "$1" ]]; then
  # A test name regex has been set, supply it to Complement
  EXTRA_COMPLEMENT_ARGS+="-run $1 "
fi

# Run the tests!
go test -v -tags synapse_blacklist,msc2403 -count=1 $EXTRA_COMPLEMENT_ARGS ./tests/...
