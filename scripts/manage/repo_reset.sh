#!/bin/bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
SCRIPTS_ROOT="$(cd "${SCRIPT_DIR}/.." && pwd)"

if [[ "${1:-}" == "--all-docker" ]]; then
    echo "WARNUNG: '--all-docker' entfernt ALLE Docker-Container, -Images, -Volumes und -Netzwerke auf diesem Host."
    echo "Dies ist destruktiv und wirkt sich auch auf andere Projekte aus."
    read -r -p "Möchten Sie wirklich ALLE Docker-Ressourcen entfernen? (ja/nein) " confirm

    if [[ "${confirm}" != "ja" ]]; then
        echo "Abbruch. Es wurden keine Docker-Ressourcen entfernt."
        exit 0
    fi

    echo "Stoppe alle laufenden Docker-Container..."
    containers="$(docker ps -aq || true)"
    if [[ -n "${containers}" ]]; then
        docker stop ${containers} 2>/dev/null || true
        docker rm ${containers} 2>/dev/null || true
    fi

    echo "Entferne alle Docker-Images..."
    images="$(docker images -aq || true)"
    if [[ -n "${images}" ]]; then
        docker rmi ${images} 2>/dev/null || true
    fi

    echo "Entferne alle Docker-Volumes..."
    volumes="$(docker volume ls -q || true)"
    if [[ -n "${volumes}" ]]; then
        docker volume rm ${volumes} 2>/dev/null || true
    fi

    echo "Entferne benutzerdefinierte Docker-Netzwerke..."
    networks="$(docker network ls -q | grep -vE '^(bridge|host|none)$' || true)"
    if [[ -n "${networks}" ]]; then
        docker network rm ${networks} 2>/dev/null || true
    fi

    echo "Bereinige verwaiste Docker-Ressourcen (system prune)..."
    docker system prune -f
    echo "Alle Docker-Ressourcen wurden bereinigt."
    exit 0
fi

echo "Stoppe PharmaLink-Compose-Stack..."
bash "${SCRIPTS_ROOT}/pharmalink_setup.sh" down || true

echo "Stoppe Hyperledger Fabric Test-Netzwerk..."
bash "${SCRIPTS_ROOT}/fabric_setup_test.sh" down || true

echo "PharmaLink-Umgebung wurde sauber zurückgesetzt."
echo "Hinweis: Für eine komplette host-weite Docker-Bereinigung nutzen Sie '$0 --all-docker'."
