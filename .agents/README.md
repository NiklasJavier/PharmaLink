# `.agents` — Repository Orientation for Agents & Contributors

> **Context:** PharmaLink is a completed hands-on demo built alongside a Bachelor's thesis to work through permissioned blockchain supply-chain traceability in practice. It is preserved as an open reference without active feature development.

## Read Order

1. [`README.md`](../README.md) (or [`README.de.md`](../README.de.md)) — overview, architecture diagram, roles, and service endpoints.
2. [`justfile`](../justfile) & [`flake.nix`](../flake.nix) — canonical command surface and reproducible devShell (`JDK 21` + `JDK 11` + `just`).
3. [`.agents/skills/pharmalink-navigator/SKILL.md`](./skills/pharmalink-navigator/SKILL.md) — module lookup table, commands, pitfalls, and boundaries.
4. [`docs/architecture.md`](../docs/architecture.md) & [`docs/domain-model.md`](../docs/domain-model.md) — CQRS sync, IPFS cache, RBAC, and on-chain states.

## Repository Map

| Path | Component | Key Entry Points |
| :--- | :--- | :--- |
| [`src/`](../src) | Spring Boot 3.5 Backend (`JDK 21`) | `PharmalinkApplication`, `FabricConfig`, `SystemStateService`, `IpfsClient`, `SecurityConfig` |
| [`frontend/`](../frontend) | Vaadin 24.8 Client UI (`JDK 21`) | `DashboardView`, `MedikamenteView`, `UnitsView`, `ActorExplorerView`, `StateService` |
| [`chaincode/pharmalink_chaincode_main/`](../chaincode/pharmalink_chaincode_main) | Hyperledger Fabric Chaincode (`JDK 11`) | `PharmaSupplyChainContract` (30 tx, 14 events, 13 errors), 7 CouchDB indexes |
| [`scripts/`](../scripts) | Fabric & Docker Lifecycle | `fabric_setEnv.sh`, `fabric_setup_test.sh`, `fabric_setup_test_consortium.sh`, `pharmalink_setup.sh`, `manage/repo_reset.sh` |
| [`docker/`](../docker) | Compose & Container Configs | `docker-compose-pharmalink.yaml`, `pharmalink/application.yaml` |
| [`common/`](../common) | Fabric Channel & CA Templates | `configtx.yaml`, `organizations/fabric-ca/` |
| [`docs/`](../docs) | Architecture, Guides & ADRs | `getting-started.md`, `architecture.md`, `domain-model.md`, `api-reference.md`, `decisions/` |
| [`evaluation/`](../evaluation) | Captured Evaluation Snapshots | `openapi/openapi.json` (28 paths / 31 ops), `blöcke.zip`, `msp/organizations.zip` |

## Command Surface (`nix` + `just`)

```bash
nix develop                 # enter reproducible shell (JDK 21, JDK 11, just, shellcheck, jq, python3, ruby)
just --list                 # inspect all recipes
just doctor                 # check tools and repo files
just check                  # compile all 3 modules + backend tests + script/JSON/YAML/docs checks
just up / just down         # boot or safely tear down full Fabric + Docker Compose test stack
```
