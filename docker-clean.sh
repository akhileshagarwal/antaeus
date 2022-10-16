#!/bin/sh

set -x

# Remove the docker volume that stores cached build artifacts.
# This also stops and removes any container using the volume.
echo 'Stopping Containers'
docker container ls --filter="name=antaeus:*" --format "table {{.ID}}"| \
 while read c; do
  docker stop "$c"
 done

echo 'Cleaning docker images'
# Remove all pleo-antaeus images.
docker images --quiet --filter="reference=pleo-antaeus:*" | \
 while read image; do
   docker rmi -f "$image"
 done

