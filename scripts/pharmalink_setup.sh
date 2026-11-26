#!/bin/bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
COMPOSE_FILE="${SCRIPT_DIR}/../docker/docker-compose-pharmalink.yaml"

GREEN='\033[0;32m'
RED='\033[0;31m'
NC='\033[0m'

usage() {
    echo "Nutzung: $0 {up|down}"
    echo "  up:   Startet die PharmaLink-Umgebung mit Docker Compose."
    echo "  down: Stoppt und bereinigt die PharmaLink-Umgebung."
}

detect_compose() {
    if docker compose version >/dev/null 2>&1; then
        COMPOSE_CMD=(docker compose)
    elif command -v docker-compose >/dev/null 2>&1; then
        COMPOSE_CMD=(docker-compose)
    else
        echo -e "${RED}Fehler: Weder 'docker compose' noch 'docker-compose' gefunden.${NC}"
        exit 1
    fi
}

start_services() {
    echo -e "${GREEN}Starte PharmaLink-Dienste...${NC}"
    if [ ! -f "${COMPOSE_FILE}" ]; then
        echo -e "${RED}Fehler: Docker-Compose-Datei nicht unter ${COMPOSE_FILE} gefunden.${NC}"
        exit 1
    fi
    detect_compose
    "${COMPOSE_CMD[@]}" -f "${COMPOSE_FILE}" up -d
    echo -e "${GREEN}Dienste gestartet. Mit 'docker ps' können Sie den Status prüfen.${NC}"
}

stop_services() {
    echo -e "${GREEN}Stoppe und bereinige PharmaLink-Dienste...${NC}"
    if [ ! -f "${COMPOSE_FILE}" ]; then
        echo -e "${RED}Fehler: Docker-Compose-Datei nicht unter ${COMPOSE_FILE} gefunden.${NC}"
        exit 1
    fi
    detect_compose
    "${COMPOSE_CMD[@]}" -f "${COMPOSE_FILE}" down
    echo -e "${GREEN}Umgebung wurde bereinigt.${NC}"
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
        echo -e "${RED}Fehler: Ungültiges Argument '${ACTION}'${NC}"
        usage
        exit 1
        ;;
esac
