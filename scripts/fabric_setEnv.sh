#!/bin/bash

# =================================================================
# Central environment variables for the PharmaLink network
# This script is loaded via `source` (no `set -euo pipefail`).
# =================================================================

if [ -n "${ZSH_VERSION:-}" ]; then
    _PHARMALINK_SELF="$(eval 'printf %s "${(%):-%x}"')"
else
    _PHARMALINK_SELF="${BASH_SOURCE[0]:-$0}"
fi
_PHARMALINK_SCRIPT_DIR="$(cd "$(dirname "${_PHARMALINK_SELF}")" && pwd)"

# --- Global Project Settings ---
export CHANNEL_NAME="pharmalink"
export CHAINCODE="pharmalink_chaincode_main"

# --- Base Paths and Endpoints ---
export BASE_DIR="${PHARMALINK_DIR:-$(cd "${_PHARMALINK_SCRIPT_DIR}/.." && pwd)}"
export ORG_DIR="${BASE_DIR}/fabric-samples/test-network/organizations"

if [ -d "${BASE_DIR}/fabric-samples/config" ]; then
    export FABRIC_CFG_PATH="${BASE_DIR}/fabric-samples/config"
else
    export FABRIC_CFG_PATH="${HOME}/fabric-cli/config"
fi

if [ -d "${BASE_DIR}/fabric-samples/bin" ] && [[ ":${PATH}:" != *":${BASE_DIR}/fabric-samples/bin:"* ]]; then
    export PATH="${BASE_DIR}/fabric-samples/bin:${PATH}"
elif [ -d "${HOME}/fabric-cli/bin" ] && [[ ":${PATH}:" != *":${HOME}/fabric-cli/bin:"* ]]; then
    export PATH="${HOME}/fabric-cli/bin:${PATH}"
fi

ENDPOINT="${FABRIC_ENDPOINT:-localhost}"

# --- Network Addresses ---
export ORDERER_ADDRESS="${ENDPOINT}:7050"
export PEER0_ORG1_ADDRESS="${ENDPOINT}:7051"
export PEER0_ORG2_ADDRESS="${ENDPOINT}:9051"

# --- CA Server Addresses ---
export CA_ORG1_ADDRESS="${ENDPOINT}:7054"
export CA_ORG2_ADDRESS="${ENDPOINT}:8054"

# --- Full URLs for CA Servers ---
export CA_ORG1_URL="https://${ENDPOINT}:7054"
export CA_ORG2_URL="https://${ENDPOINT}:8054"

# --- Identity-Specific Settings (Default: Org1 Admin) ---
export CORE_PEER_LOCALMSPID="Org1MSP"
export CORE_PEER_ADDRESS="${PEER0_ORG1_ADDRESS}"
export CORE_PEER_MSPCONFIGPATH="${ORG_DIR}/peerOrganizations/org1.example.com/users/Admin@org1.example.com/msp"
export CORE_PEER_TLS_ENABLED=true

# --- TLS Certificate Paths ---
export ORDERER_CA="${ORG_DIR}/ordererOrganizations/example.com/orderers/orderer.example.com/msp/tlscacerts/tlsca.example.com-cert.pem"
export PEER0_ORG1_CA="${ORG_DIR}/peerOrganizations/org1.example.com/peers/peer0.org1.example.com/tls/ca.crt"
export PEER0_ORG2_CA="${ORG_DIR}/peerOrganizations/org2.example.com/peers/peer0.org2.example.com/tls/ca.crt"
export CORE_PEER_TLS_ROOTCERT_FILE="${PEER0_ORG1_CA}"

# --- Specific Paths for fabric-ca-client ---
export FABRIC_CA_CLIENT_HOME_ORG1="${ORG_DIR}/peerOrganizations/org1.example.com/"
export FABRIC_CA_CLIENT_HOME_ORG2="${ORG_DIR}/peerOrganizations/org2.example.com/"
export CA_ORG1_TLS_CERTFILE="${ORG_DIR}/peerOrganizations/org1.example.com/ca/ca.org1.example.com-cert.pem"
export CA_ORG2_TLS_CERTFILE="${ORG_DIR}/peerOrganizations/org2.example.com/ca/ca.org2.example.com-cert.pem"

echo "Base environment variables set for PharmaLink (BASE_DIR=${BASE_DIR})."
