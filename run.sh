#!/usr/bin/env sh
set -e

set -a
. ./.env
set +a

echo "Starting RMR SURPLUS desktop app..."
./mvnw -pl inventory-desktop javafx:run
