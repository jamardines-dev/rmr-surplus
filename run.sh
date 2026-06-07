#!/usr/bin/env sh
set -a
. ./.env
set +a

./mvnw javafx:run
