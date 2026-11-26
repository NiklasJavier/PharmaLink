#!/bin/bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
SCRIPTS_ROOT="$(cd "${SCRIPT_DIR}/.." && pwd)"

if [[ "${1:-}" == "--all-docker" ]]; then
    echo "WARNING: '--all-docker' removes ALL Docker containers, images, volumes, and networks on this host."
    echo "This is destructive and also affects other projects."
    read -r -p "Are you sure you want to remove ALL Docker resources? (yes/no) " confirm

    if [[ "${confirm}" != "yes" ]]; then
        echo "Aborted. No Docker resources were removed."
        exit 0
    fi

    echo "Stopping all running Docker containers..."
    containers="$(docker ps -aq || true)"
    if [[ -n "${containers}" ]]; then
        docker stop ${containers} 2>/dev/null || true
        docker rm ${containers} 2>/dev/null || true
    fi

    echo "Removing all Docker images..."
    images="$(docker images -aq || true)"
    if [[ -n "${images}" ]]; then
        docker rmi ${images} 2>/dev/null || true
    fi

    echo "Removing all Docker volumes..."
    volumes="$(docker volume ls -q || true)"
    if [[ -n "${volumes}" ]]; then
        docker volume rm ${volumes} 2>/dev/null || true
    fi

    echo "Removing custom Docker networks..."
    networks="$(docker network ls -q | grep -vE '^(bridge|host|none)$' || true)"
    if [[ -n "${networks}" ]]; then
        docker network rm ${networks} 2>/dev/null || true
    fi

    echo "Pruning dangling Docker resources (system prune)..."
    docker system prune -f
    echo "All Docker resources have been cleaned up."
    exit 0
fi

echo "Stopping PharmaLink Compose stack..."
bash "${SCRIPTS_ROOT}/pharmalink_setup.sh" down || true

echo "Stopping Hyperledger Fabric test network..."
bash "${SCRIPTS_ROOT}/fabric_setup_test.sh" down || true

echo "PharmaLink environment has been cleanly reset."
echo "Note: For a complete host-wide Docker cleanup, use '$0 --all-docker'."
