#!/bin/bash

if [ -n "${ZSH_VERSION:-}" ]; then
    _ROLE_SELF="$(eval 'printf %s "${(%):-%x}"')"
    case "${ZSH_EVAL_CONTEXT:-}" in *:file*) _ROLE_IS_SOURCED=1 ;; *) _ROLE_IS_SOURCED=0 ;; esac
else
    _ROLE_SELF="${BASH_SOURCE[0]:-$0}"
    [ "${_ROLE_SELF}" != "${0}" ] && _ROLE_IS_SOURCED=1 || _ROLE_IS_SOURCED=0
fi

_ROLE_SCRIPT_DIR="$(cd "$(dirname "${_ROLE_SELF}")" && pwd)"
# shellcheck source=../fabric_setEnv.sh
source "${_ROLE_SCRIPT_DIR}/../fabric_setEnv.sh"

export ORG1_DIR="${ORG_DIR}/peerOrganizations/org1.example.com"
export USER_DIR="${ORG1_DIR}/users/hersteller-user1@org1.example.com"
export CORE_PEER_MSPCONFIGPATH="${USER_DIR}/msp"
export CORE_PEER_LOCALMSPID="Org1MSP"
export CORE_PEER_TLS_ENABLED=true
export CORE_PEER_ADDRESS="${PEER0_ORG1_ADDRESS}"

echo "Prüfe Umgebungsvariablen (hersteller-user1):"
echo "CORE_PEER_MSPCONFIGPATH: ${CORE_PEER_MSPCONFIGPATH}"
echo "CORE_PEER_LOCALMSPID: ${CORE_PEER_LOCALMSPID}"
echo "CORE_PEER_TLS_ENABLED: ${CORE_PEER_TLS_ENABLED}"
echo "CORE_PEER_ADDRESS: ${CORE_PEER_ADDRESS}"

if [ "${_ROLE_IS_SOURCED}" -eq 0 ]; then
    peer chaincode invoke -o "${ORDERER_ADDRESS}" --tls --cafile "${ORDERER_CA}" -C "${CHANNEL_NAME}" -n "${CHAINCODE}" \
        --peerAddresses "${PEER0_ORG1_ADDRESS}" --tlsRootCertFiles "${PEER0_ORG1_CA}" \
        --peerAddresses "${PEER0_ORG2_ADDRESS}" --tlsRootCertFiles "${PEER0_ORG2_CA}" \
        -c '{"function":"initCall","Args":[""]}'
fi
