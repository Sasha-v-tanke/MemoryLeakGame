#!/usr/bin/env bash
set -euo pipefail

CONTAINER_NAME="memory-leak-postgres"

if docker ps --format '{{.Names}}' | grep -qx "$CONTAINER_NAME"; then
  echo "PostgreSQL is already running in container $CONTAINER_NAME"
  exit 0
fi

if docker ps -a --format '{{.Names}}' | grep -qx "$CONTAINER_NAME"; then
  docker start "$CONTAINER_NAME"
  echo "PostgreSQL container $CONTAINER_NAME started"
  exit 0
fi

docker run \
  --name "$CONTAINER_NAME" \
  -e POSTGRES_DB=game \
  -e POSTGRES_USER=gameuser \
  -e POSTGRES_PASSWORD=password \
  -p 5432:5432 \
  -d postgres:16

echo "PostgreSQL container $CONTAINER_NAME created and started"
