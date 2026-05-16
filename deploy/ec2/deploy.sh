#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
cd "${ROOT_DIR}"

docker compose pull
docker compose up -d --no-build

if [[ "${ENABLE_SONARQUBE:-false}" == "true" ]]; then
  docker compose --profile tools pull sonarqube-db sonarqube
  docker compose --profile tools up -d --no-build sonarqube-db sonarqube
fi

docker image prune -f

