#!/bin/bash
export DOCKER_CLI_EXPERIMENTAL=enabled
export DOCKER_BUILDKIT=1
docker run --rm --privileged multiarch/qemu-user-static --reset -p yes
docker buildx create --use --name armbuilder
docker buildx inspect --bootstrap
docker buildx build --platform linux/arm64 --load -t totalcross/linux-arm64-build .
