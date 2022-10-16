#!/bin/sh

set -x

# Create a new image version with latest code changes.
docker build . --tag pleo-antaeus

# Build the code.
docker-compose up