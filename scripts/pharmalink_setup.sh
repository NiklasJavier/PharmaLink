#!/bin/bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
COMPOSE_FILE="${SCRIPT_DIR}/../docker/docker-compose-pharmalink.yaml"

GREEN='\033[0;32m'
RED='\033[0;31m'
NC='\033[0m'

usage() {
    echo "Usage: $0 {up|down}"
    echo "  up:   Starts the PharmaLink environment with Docker Compose."
    echo "  down: Stops and cleans up the PharmaLink environment."
}

detect_compose() {
    if docker compose version >/dev/null 2>&1; then
        COMPOSE_CMD=(docker compose)
    elif command -v docker-compose >/dev/null 2>&1; then
        COMPOSE_CMD=(docker-compose)
    else
        echo -e "${RED}Error: Neither 'docker compose' nor 'docker-compose' found.${NC}"
        exit 1
    fi
}

start_services() {
    echo -e "${GREEN}Starting PharmaLink services...${NC}"
    if [ ! -f "${COMPOSE_FILE}" ]; then
        echo -e "${RED}Error: Docker Compose file not found at ${COMPOSE_FILE}.${NC}"
        exit 1
    fi
    detect_compose
    "${COMPOSE_CMD[@]}" -f "${COMPOSE_FILE}" up -d
    echo -e "${GREEN}Services started. Check status with 'docker ps'.${NC}"
}

stop_services() {
    echo -e "${GREEN}Stopping and cleaning up PharmaLink services...${NC}"
    if [ ! -f "${COMPOSE_FILE}" ]; then
        echo -e "${RED}Error: Docker Compose file not found at ${COMPOSE_FILE}.${NC}"
        exit 1
    fi
    detect_compose
    "${COMPOSE_CMD[@]}" -f "${COMPOSE_FILE}" down
    echo -e "${GREEN}Environment cleaned up.${NC}"
}

if [ "$#" -ne 1 ]; then
    usage
    exit 1
fi

ACTION="$1"

case "${ACTION}" in
    up)
        start_services
        ;;
    down)
        stop_services
        ;;
    *)
        echo -e "${RED}Error: Invalid argument '${ACTION}'${NC}"
        usage
        exit 1
        ;;
esac
