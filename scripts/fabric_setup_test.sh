#!/bin/bash
set -euo pipefail

SRC_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" >/dev/null 2>&1 && pwd)"
# shellcheck source=./fabric_setEnv.sh
source "${SRC_DIR}/fabric_setEnv.sh"

FABRIC_VERSION="2.5.13"
CA_VERSION="1.5.15"

function networkDown() {
  if [[ -d "${SRC_DIR}/../fabric-samples/test-network" ]]; then
    (
      cd "${SRC_DIR}/../fabric-samples/test-network"
      ./network.sh down
    )
    rm -rf "${SRC_DIR}/../fabric-samples"
  fi
}

function networkUp() {
  networkDown
  (
    cd "${SRC_DIR}/.."
    curl -fsSLO https://raw.githubusercontent.com/hyperledger/fabric/main/scripts/install-fabric.sh
    chmod +x install-fabric.sh
    ./install-fabric.sh --fabric-version "${FABRIC_VERSION}" --ca-version "${CA_VERSION}"

    cp -r ./common/organizations ./fabric-samples/test-network/

    cd fabric-samples
    if [[ "${FABRIC_VERSION}" =~ ^2\.2\.[0-9]+$ ]]; then
      git checkout release-2.2
    fi
    cd test-network
    ./network.sh up createChannel -ca -c pharmalink -s couchdb
  )
}

function deployChaincode() {
  echo "## 1. Compiling and packaging chaincode with Gradle..."
  local gradle_cmd="${SRC_DIR}/../gradlew"
  if [ ! -x "${gradle_cmd}" ]; then
    gradle_cmd="gradle"
  fi

  "${gradle_cmd}" -p "${SRC_DIR}/../chaincode/pharmalink_chaincode_main/" installDist -x jacocoTestCoverageVerification
  echo "## Gradle build successful."

  echo ""
  echo "## 2. Deploying chaincode to the Fabric network..."
  "${SRC_DIR}/../fabric-samples/test-network/network.sh" deployCC \
    -c pharmalink \
    -ccn pharmalink_chaincode_main \
    -ccp "${SRC_DIR}/../chaincode/pharmalink_chaincode_main" \
    -ccl java
  echo "## Chaincode deployment successful."
}

function printHelp() {
  echo "Usage:"
  echo "  $0 up [-v <fabric_version> [ca_version]]"
  echo "  $0 cc"
  echo "  $0 down"
}

if [[ $# -lt 1 ]]; then
  printHelp
  exit 0
fi

MODE="$1"
shift

while [[ $# -ge 1 ]]; do
  key="$1"
  case "${key}" in
    -h)
      printHelp
      exit 0
      ;;
    -v)
      FABRIC_VERSION="${2:?Missing Fabric version after -v}"
      if [[ -n "${3:-}" && "${3:-}" != -* ]]; then
        CA_VERSION="$3"
        shift
      fi
      shift
      ;;
    *)
      shift
      ;;
  esac
done

case "${MODE}" in
  up)
    networkUp
    ;;
  down)
    networkDown
    ;;
  cc)
    deployChaincode
    ;;
  *)
    printHelp
    exit 1
    ;;
esac
