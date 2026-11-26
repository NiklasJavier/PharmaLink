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
        echo "Error: Unsupported operating system '${OS}'. Supported: Linux and macOS (Darwin)."
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
        echo "Error: Unsupported architecture '${ARCH}'. Supported: x86_64 (amd64) and aarch64/arm64."
        exit 1
        ;;
esac

FABRIC_PLATFORM="${FABRIC_OS}-${FABRIC_ARCH}"
FABRIC_TAR_GZ_URL="https://github.com/hyperledger/fabric/releases/download/v${FABRIC_VERSION}/hyperledger-fabric-${FABRIC_PLATFORM}-${FABRIC_VERSION}.tar.gz"
DOWNLOAD_FILENAME="hyperledger-fabric-${FABRIC_PLATFORM}-${FABRIC_VERSION}.tar.gz"
ARCHIVE_PATH="${HOME}/${DOWNLOAD_FILENAME}"

echo "Starting setup of Hyperledger Fabric binaries..."
echo "Platform: ${FABRIC_PLATFORM}"
echo "Download URL: ${FABRIC_TAR_GZ_URL}"
echo "Installation directory: ${INSTALL_DIR}"

echo "1. Downloading Hyperledger Fabric binaries..."
curl -fL "${FABRIC_TAR_GZ_URL}" -o "${ARCHIVE_PATH}"
echo "Download complete: ${ARCHIVE_PATH}"

if [ -d "${INSTALL_DIR}" ]; then
    echo "2. Removing existing installation directory '${INSTALL_DIR}'..."
    rm -rf "${INSTALL_DIR}"
fi

echo "3. Extracting Fabric binaries to '${INSTALL_DIR}'..."
mkdir -p "${INSTALL_DIR}"
tar -xzf "${ARCHIVE_PATH}" -C "${INSTALL_DIR}"
echo "Extraction complete."

echo "4. Deleting downloaded tar archive: ${ARCHIVE_PATH}..."
rm -f "${ARCHIVE_PATH}"

echo "5. Adding the Fabric 'bin' path to the shell configuration..."

SHELL_CONFIG_FILE=""
case "${SHELL:-}" in
    */zsh)
        SHELL_CONFIG_FILE="${HOME}/.zshrc"
        ;;
    */bash)
        SHELL_CONFIG_FILE="${HOME}/.bashrc"
        ;;
    *)
        echo "Warning: Unknown shell '${SHELL:-}'. Using ~/.bashrc as fallback."
        SHELL_CONFIG_FILE="${HOME}/.bashrc"
        ;;
esac

touch "${SHELL_CONFIG_FILE}"
if ! grep -q "export PATH=\"\$PATH:${BIN_PATH}\"" "${SHELL_CONFIG_FILE}"; then
    printf '\n# Add Hyperledger Fabric binaries to PATH\nexport PATH="$PATH:%s"\n' "${BIN_PATH}" >> "${SHELL_CONFIG_FILE}"
    echo "${SHELL_CONFIG_FILE} has been updated."
else
    echo "The Fabric 'bin' path is already present in ${SHELL_CONFIG_FILE}. No changes made."
fi

echo ""
echo "---------------------------------------------------------"
echo "Hyperledger Fabric binaries setup complete."
echo "Open a NEW terminal or run:"
echo "  source \"${SHELL_CONFIG_FILE}\""
echo ""
echo "Test the installation with:"
echo "  peer version"
echo "  fabric-ca-client version"
echo "---------------------------------------------------------"
