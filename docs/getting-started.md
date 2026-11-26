# Getting Started

This guide covers full local setup of the Hyperledger Fabric test network, Java chaincode, Spring Boot backend, Vaadin 24 client UI, IPFS node, and MongoDB stack, plus an IDE-friendly development setup.

## 1. Prerequisites

| Tool | Required Version | Purpose |
| :--- | :--- | :--- |
| **Docker & Docker Compose** | Docker Engine + Compose v2 (or `docker-compose` v1) | Fabric peers, orderer, CouchDB, MongoDB, Kubo IPFS, app containers |
| **Nix (Recommended)** | Nix with Flakes (`nix develop`) | Reproducible dev shell providing **JDK 21**, **JDK 11**, `just`, `shellcheck`, `jq`, `curl`, `zip`, `unzip` |
| **Java (Manual Fallback)** | **JDK 11** (chaincode) + **JDK 21** (backend & Vaadin UI) | Used when running outside `nix develop` |

### Recommended: Nix + `just` Dev Shell

Install Nix with Flakes enabled once on your machine:

- **macOS / Linux:**
  ```bash
  curl --proto '=https' --tlsv1.2 -sSf -L https://install.determinate.systems/nix | sh -s -- install
  ```
  (Official alternative: [nixos.org/download](https://nixos.org/download/).)
- **Windows:** Enable [WSL2](https://learn.microsoft.com/en-us/windows/wsl/install) and turn on **Docker Desktop -> Settings -> Resources -> WSL Integration**, then run the same Linux Nix installer above inside your WSL2 distribution (Fabric test-network Bash scripts require a POSIX shell).

Then enter the declarative dev shell from `flake.nix` (or let `direnv allow` load `.envrc` automatically):

```bash
nix develop
just --list
just doctor
just check
```

From inside `nix develop` (or via `nix develop -c just ...`), `just up` and `just down` manage the complete Fabric + Docker Compose stack, and `just` automatically passes `JDK21_HOME` and `JDK11_HOME` to Gradle.

### Manual Fallback: Installing Java 11 and Java 21 with SDKMAN

```bash
curl -s "https://get.sdkman.io" | bash
source "$HOME/.sdkman/bin/sdkman-init.sh"

sdk install java 11.0.27-tem
sdk install java 21.0.7-tem
sdk default java 21.0.7-tem
```

> **Toolchain note:** The chaincode module (`chaincode/pharmalink_chaincode_main/build.gradle`) pins `JavaLanguageVersion.of(11)`. If you do not use `nix develop`, ensure JDK 11 is installed locally when building or deploying chaincode.

### Optional: Standalone Fabric CLI Binaries

If you want `peer` and `fabric-ca-client` available outside `fabric-samples/`:

```bash
./scripts/fabric_setup_cli.sh
```

The script detects Linux/macOS and `amd64`/`arm64`, installs binaries into `~/fabric-cli`, and appends `PATH` to `~/.zshrc` or `~/.bashrc`.

---

## 2. Full Local Stack Startup

Run these commands from the repository root (`PharmaLink/`):

```bash
# 1. Load Fabric environment variables (resolves repository root automatically)
source ./scripts/fabric_setEnv.sh

# 2. Download Fabric v2.5.13 / CA v1.5.15 binaries and boot the test-network with CouchDB
./scripts/fabric_setup_test.sh up

# 3. Build Java chaincode (JDK 11) and deploy to channel 'pharmalink'
./scripts/fabric_setup_test.sh cc

# 4. Register consortium identities (3 users per role) and invoke initCall for each
./scripts/fabric_setup_test_consortium.sh

# 5. Start Spring Boot backend, Vaadin UI, MongoDB, and IPFS Kubo
./scripts/pharmalink_setup.sh up
```

Check running containers with:

```bash
docker ps --format "table {{.Names}}\t{{.Status}}\t{{.Ports}}"
```

### End-to-End Live Smoke Verification

Once `just up` (or the manual steps above) finishes, verify authentication, backend ledger cache sync, and the Vaadin frontend:

```bash
TOKEN=$(curl -fsS -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"admin123"}' | jq -r '.jwt')

curl -fsS http://localhost:8080/api/system/cache/stats \
  -H "Authorization: Bearer $TOKEN" | jq .
```

Then open [http://localhost:3000](http://localhost:3000) (Vaadin UI) and [http://localhost:8080/api/swagger](http://localhost:8080/api/swagger) (Swagger UI). Tear down cleanly with `just down`.

---

## 3. IDE / Local Development Workflow

When iterating on Java code without rebuilding Docker images:

1. Start only the backing MongoDB and IPFS containers from `src/main/dev/docker-compose.yaml` (or reuse the IntelliJ run config `.run/PharmalinkDevEnv.run.xml`):

   ```bash
   docker compose -f ./src/main/dev/docker-compose.yaml up -d
   ```

2. Ensure the Fabric test network is running and `./fabric-samples/test-network/organizations` exists (or override `FABRIC_CRYPTO_PATH` and `FABRIC_PEER_ENDPOINT`).
3. Start the Spring Boot backend (port `8080`):

   ```bash
   ./gradlew bootRun
   ```

4. Start the Vaadin 24 client UI (port `8083`, pointing to `http://localhost:8080/api`):

   ```bash
   ./gradlew -p frontend bootRun
   ```

IntelliJ run definitions are included under `.run/PharmalinkApplication.run.xml` and `.run/PharmalinkDevEnv.run.xml`.

---

## 4. Configuration & Environment Variables

Both `src/main/resources/application.yaml` and `docker/pharmalink/application.yaml` expose environment variable overrides with local test defaults.

### Backend (`src/main/resources/application.yaml` & Docker)

| Variable | Local Default | Docker Default | Description |
| :--- | :--- | :--- | :--- |
| `MONGODB_URI` | `mongodb://user:user@localhost:27017/pharmalinkdb?authSource=admin` | `mongodb://user:user@mongodb:27017/pharmalinkdb?authSource=admin` | MongoDB connection string |
| `JWT_SECRET` | Base64 dev secret | Base64 dev secret | HMAC signing key for JWTs (override outside local tests) |
| `JWT_EXPIRATION` | `3600000` | `3600000` | JWT validity in milliseconds (1 hour) |
| `SYSTEM_USERNAME` | `admin` | `admin` | Initial application admin username |
| `SYSTEM_PASSWORD` | `admin123` | `admin123` | Initial application admin password |
| `ACTOR_NAME` | `company2` | `company2` | Node actor label metadata |
| `FABRIC_MSP_ID` | `Org1MSP` | `Org1MSP` | Hyperledger Fabric MSP ID |
| `FABRIC_CRYPTO_PATH` | `./fabric-samples/test-network/organizations/peerOrganizations/org1.example.com` | `/fabric-crypto/organizations/peerOrganizations/org1.example.com` | Org MSP crypto directory |
| `FABRIC_CERT_PATH` | `users/hersteller-user1@org1.example.com/msp/signcerts` | `users/hersteller-user1@org1.example.com/msp/signcerts` | Signing certificate dir relative to `FABRIC_CRYPTO_PATH` |
| `FABRIC_KEY_DIR_PATH` | `users/hersteller-user1@org1.example.com/msp/keystore` | `users/hersteller-user1@org1.example.com/msp/keystore` | Private key dir relative to `FABRIC_CRYPTO_PATH` |
| `FABRIC_TLS_CERT_PATH` | `peers/peer0.org1.example.com/tls/ca.crt` | `peers/peer0.org1.example.com/tls/ca.crt` | Peer TLS root CA cert relative to `FABRIC_CRYPTO_PATH` |
| `FABRIC_PEER_ENDPOINT` | `localhost:7051` | `host.docker.internal:7051` | gRPC target for Fabric Gateway |
| `FABRIC_PEER_OVERRIDE_AUTH` | `peer0.org1.example.com` | `peer0.org1.example.com` | TLS authority override for peer certs |
| `FABRIC_CHANNEL_NAME` | `pharmalink` | `pharmalink` | Target Fabric channel |
| `FABRIC_CHAINCODE_NAME` | `pharmalink_chaincode_main` | `pharmalink_chaincode_main` | Deployed chaincode identifier |
| `IPFS_HOST` | `localhost` | `ipfs` | IPFS Kubo API hostname |
| `IPFS_PORT` | `5001` | `5001` | IPFS RPC API port |
| `IPFS_TIMEOUT` | `3` | `3` | IPFS HTTP client timeout in seconds |

### Frontend (`frontend/src/main/resources/application.properties`)

| Variable | Local Default | Docker Default | Description |
| :--- | :--- | :--- | :--- |
| `PORT` | `8083` | `8083` (mapped to host `3000`) | Vaadin server port |
| `BACKEND_BASEURL` | `http://localhost:8080/api` | `http://app-pharmalink:8080/api` | Base URL for backend REST calls |

---

## 5. Service Endpoints & Ports

| Service | Host Port / URL | Purpose |
| :--- | :--- | :--- |
| **Vaadin 24 Client UI** | `http://localhost:3000` (Compose) / `http://localhost:8083` (IDE) | Primary web client (`admin` / `admin123`) |
| **Spring Boot REST API** | `http://localhost:8080/api/v1` | JWT-authenticated API endpoints |
| **Swagger UI** | `http://localhost:8080/api/swagger` (or `/swagger`) | Interactive OpenAPI browser |
| **Thymeleaf Diagnostics UI** | `http://localhost:8080/app/dashboard` | Embedded node audit & cache overview (`admin` / `admin123`) |
| **IPFS RPC / Gateway** | `http://localhost:5001` / `http://localhost:8081` | Kubo v0.29.0 API and HTTP gateway |
| **MongoDB** | `mongodb://localhost:27017` | Read-model, IPFS cache, audit logs (`user` / `user`) |
| **Fabric Orderer** | `localhost:7050` | Raft ordering service |
| **Fabric Org1 / Org2 Peers** | `localhost:7051` / `localhost:9051` | Endorsing/committing peers |
| **Fabric CA Org1 / Org2** | `https://localhost:7054` / `https://localhost:8054` | Test-network certificate authorities |

---

## 6. Switching Actor Identities via CLI

All 12 role scripts under `scripts/roles/` support two modes:

- **Switch identity in your shell (`source`):** sets `CORE_PEER_MSPCONFIGPATH`, `CORE_PEER_LOCALMSPID`, `CORE_PEER_ADDRESS`, and TLS variables without submitting a transaction.
- **Execute `initCall` (`bash`):** switches identity and invokes `initCall` on the chaincode (used automatically by `fabric_setup_test_consortium.sh`).

Available role scripts (`user1`–`user3` per role):
- `scripts/roles/fabric_role_hersteller.sh` (`hersteller2.sh`, `hersteller3.sh`)
- `scripts/roles/fabric_role_grosshaendler.sh` (`grosshaendler2.sh`, `grosshaendler3.sh`)
- `scripts/roles/fabric_role_apotheke.sh` (`apotheke2.sh`, `apotheke3.sh`)
- `scripts/roles/fabric_role_behoerde.sh` (`behoerde2.sh`, `behoerde3.sh`)

Example:

```bash
source ./scripts/roles/fabric_role_behoerde.sh

peer chaincode query -C "$CHANNEL_NAME" -n "$CHAINCODE" \
  -c '{"function":"queryAllActors","Args":[]}'
```

---

## 7. Teardown & Troubleshooting

### Safe Teardown

```bash
# Stops Compose services and tears down fabric-samples/test-network
./scripts/manage/repo_reset.sh
```

Only use `--all-docker` if you intentionally want to wipe **every** Docker container, image, volume, and custom network on your host:

```bash
./scripts/manage/repo_reset.sh --all-docker
```

### Common Issues

| Symptom | Cause | Fix |
| :--- | :--- | :--- |
| Chaincode build fails looking for JDK 11 | Local environment only has JDK 17/21+ | Install JDK 11 via SDKMAN (`sdk install java 11.0.27-tem`) |
| Chaincode `build`/`installDist` fails on JaCoCo | `build.gradle` enforces 90% coverage while no unit tests exist yet | Scripts pass `-x jacocoTestCoverageVerification` automatically |
| Backend fails startup on `TLS Zertifikats-Datei nicht gefunden` | Fabric test-network has not generated `fabric-samples/test-network/organizations` yet | Run `./scripts/fabric_setup_test.sh up` before starting the backend |
| Backend starts before `initCall` has run | `AppDataInitializer` logs warning and continues without cached `actorId` | Run `./scripts/fabric_setup_test_consortium.sh` or restart backend after chaincode init |
| Docker backend cannot reach Peer on Linux | `host.docker.internal` not resolved on older Docker engines | Compose configures `extra_hosts: host.docker.internal:host-gateway` (Docker >= 20.10 required) |
