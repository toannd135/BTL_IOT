#!/usr/bin/env bash
# Starts the backend with the settings from .env (falling back to .env.example).
set -euo pipefail
cd "$(dirname "$0")"

ENV_FILE=".env"
[ -f "$ENV_FILE" ] || ENV_FILE=".env.example"
echo "Loading configuration from $ENV_FILE"

set -a
# shellcheck disable=SC1090
source "$ENV_FILE"
set +a

exec mvn spring-boot:run
