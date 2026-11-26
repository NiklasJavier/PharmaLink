#!/bin/bash
set -euo pipefail

FABRIC_VERSION="${FABRIC_VERSION:-2.5.13}"
INSTALL_DIR="${HOME}/fabric-cli"
BIN_PATH="${INSTALL_DIR}/bin"

OS="$(uname -s)"
case "${OS}" in
    Linux)
        FABRIC_OS="linux"
        ;;
    Darwin)
        FABRIC_OS="darwin"
        ;;
    *)
        echo "Fehler: Nicht unterstütztes Betriebssystem '${OS}'. Unterstützt werden: Linux und macOS (Darwin)."
        exit 1
        ;;
esac

ARCH="$(uname -m)"
case "${ARCH}" in
    x86_64|amd64)
        FABRIC_ARCH="amd64"
        ;;
    aarch64|arm64)
        FABRIC_ARCH="arm64"
        ;;
    *)
        echo "Fehler: Nicht unterstützte Architektur '${ARCH}'. Unterstützt werden: x86_64 (amd64) und aarch64/arm64."
        exit 1
        ;;
esac

FABRIC_PLATFORM="${FABRIC_OS}-${FABRIC_ARCH}"
FABRIC_TAR_GZ_URL="https://github.com/hyperledger/fabric/releases/download/v${FABRIC_VERSION}/hyperledger-fabric-${FABRIC_PLATFORM}-${FABRIC_VERSION}.tar.gz"
DOWNLOAD_FILENAME="hyperledger-fabric-${FABRIC_PLATFORM}-${FABRIC_VERSION}.tar.gz"
ARCHIVE_PATH="${HOME}/${DOWNLOAD_FILENAME}"

echo "Starte Einrichtung der Hyperledger Fabric Binaries..."
echo "Plattform: ${FABRIC_PLATFORM}"
echo "Download-URL: ${FABRIC_TAR_GZ_URL}"
echo "Installationsverzeichnis: ${INSTALL_DIR}"

echo "1. Lade Hyperledger Fabric Binaries herunter..."
curl -fL "${FABRIC_TAR_GZ_URL}" -o "${ARCHIVE_PATH}"
echo "Download abgeschlossen: ${ARCHIVE_PATH}"

if [ -d "${INSTALL_DIR}" ]; then
    echo "2. Vorhandenes Installationsverzeichnis '${INSTALL_DIR}' wird entfernt..."
    rm -rf "${INSTALL_DIR}"
fi

echo "3. Entpacke Fabric Binaries nach '${INSTALL_DIR}'..."
mkdir -p "${INSTALL_DIR}"
tar -xzf "${ARCHIVE_PATH}" -C "${INSTALL_DIR}"
echo "Entpacken abgeschlossen."

echo "4. Lösche die heruntergeladene Tar-Datei: ${ARCHIVE_PATH}..."
rm -f "${ARCHIVE_PATH}"

echo "5. Binde den Fabric 'bin'-Pfad in die Shell-Konfiguration ein..."

SHELL_CONFIG_FILE=""
case "${SHELL:-}" in
    */zsh)
        SHELL_CONFIG_FILE="${HOME}/.zshrc"
        ;;
    */bash)
        SHELL_CONFIG_FILE="${HOME}/.bashrc"
        ;;
    *)
        echo "Warnung: Unbekannte Shell '${SHELL:-}'. Verwende ~/.bashrc als Fallback."
        SHELL_CONFIG_FILE="${HOME}/.bashrc"
        ;;
esac

touch "${SHELL_CONFIG_FILE}"
if ! grep -q "export PATH=\"\$PATH:${BIN_PATH}\"" "${SHELL_CONFIG_FILE}"; then
    printf '\n# Add Hyperledger Fabric binaries to PATH\nexport PATH="$PATH:%s"\n' "${BIN_PATH}" >> "${SHELL_CONFIG_FILE}"
    echo "${SHELL_CONFIG_FILE} wurde aktualisiert."
else
    echo "Der Fabric 'bin'-Pfad ist bereits in ${SHELL_CONFIG_FILE} vorhanden. Keine Änderung vorgenommen."
fi

echo ""
echo "---------------------------------------------------------"
echo "Einrichtung der Hyperledger Fabric Binaries abgeschlossen."
echo "Öffnen Sie ein NEUES Terminal oder führen Sie aus:"
echo "  source \"${SHELL_CONFIG_FILE}\""
echo ""
echo "Testen Sie die Installation mit:"
echo "  peer version"
echo "  fabric-ca-client version"
echo "---------------------------------------------------------"
