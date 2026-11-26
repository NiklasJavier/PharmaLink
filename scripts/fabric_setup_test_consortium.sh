#!/bin/bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

echo "--> Loading base configuration from fabric_setEnv.sh..."
# shellcheck source=./fabric_setEnv.sh
source "${SCRIPT_DIR}/fabric_setEnv.sh"

FABRIC_CA_CLIENT_PATH="${BASE_DIR}/fabric-samples/bin/fabric-ca-client"
NUM_USERS_PER_ROLE=3
AFFILIATIONS=("hersteller" "grosshaendler" "apotheke" "behoerde")

if [ ! -f "${FABRIC_CA_CLIENT_PATH}" ]; then
    echo "ERROR: fabric-ca-client not found at: ${FABRIC_CA_CLIENT_PATH}"
    exit 1
fi

export FABRIC_CA_CLIENT_HOME="${FABRIC_CA_CLIENT_HOME_ORG1}"
CA_TLS_CERTFILE="${CA_ORG1_TLS_CERTFILE}"
CA_SERVER_URL="${CA_ORG1_URL}"

echo "INFO: All identities will be created with the central CA at ${CA_SERVER_URL}."

echo -e "\n1. Logging in as CA admin for Org1..."
"${FABRIC_CA_CLIENT_PATH}" enroll -u "${CA_SERVER_URL/https:\/\//https:\/\/admin:adminpw@}" --tls.certfiles "${CA_TLS_CERTFILE}"

ADMIN_CERT_PATH="${ORG_DIR}/peerOrganizations/org1.example.com/users/Admin@org1.example.com/msp/signcerts/cert.pem"

if [ ! -f "${ADMIN_CERT_PATH}" ]; then
    echo "ERROR: Org1 admin certificate not found at: ${ADMIN_CERT_PATH}"
    echo "Make sure the Org1 admin identity has already been created (e.g. via registerEnroll.sh)."
    exit 1
fi

for aff in "${AFFILIATIONS[@]}"; do
  echo -e "\n========================================================"
  echo "Processing: '${aff}' in Org1"
  echo "========================================================"

  AFFILIATION_PATH="org1.${aff}"
  echo "--> Creating affiliation: '${AFFILIATION_PATH}'..."
  "${FABRIC_CA_CLIENT_PATH}" affiliation add "${AFFILIATION_PATH}" --force --tls.certfiles "${CA_TLS_CERTFILE}"

  echo "--> Registering and enrolling ${NUM_USERS_PER_ROLE} users for role '${aff}'..."
  for i in $(seq 1 "${NUM_USERS_PER_ROLE}"); do
    USERNAME="${aff}-user${i}"
    PASSWORD="${aff}-pw${i}"

    echo -e "\n    -> Processing '${USERNAME}'..."

    echo "       -> Registering '${USERNAME}' with the CA..."
    "${FABRIC_CA_CLIENT_PATH}" register \
      --id.name "${USERNAME}" \
      --id.secret "${PASSWORD}" \
      --id.type client \
      --id.affiliation "${AFFILIATION_PATH}" \
      --id.attrs "role=${aff}:ecert" \
      --tls.certfiles "${CA_TLS_CERTFILE}"

    MSP_DIR="${ORG_DIR}/peerOrganizations/org1.example.com/users/${USERNAME}@org1.example.com/msp"
    echo "       -> Enrolling '${USERNAME}' and saving certificates to:"
    echo "          ${MSP_DIR}"

    "${FABRIC_CA_CLIENT_PATH}" enroll \
      -u "https://${USERNAME}:${PASSWORD}@${CA_SERVER_URL#https://}" \
      --mspdir "${MSP_DIR}" \
      --tls.certfiles "${CA_TLS_CERTFILE}"

    echo "       -> Creating 'admincerts' directory and copying admin certificate..."
    mkdir -p "${MSP_DIR}/admincerts"
    cp "${ADMIN_CERT_PATH}" "${MSP_DIR}/admincerts/org1-admin-cert.pem"
  done
done

for file in "${SCRIPT_DIR}/roles/"*.sh; do
  echo "Running initCall script: ${file}"
  bash "${file}"
done

echo "All identities for the PharmaLink consortium have been successfully created and enrolled."
