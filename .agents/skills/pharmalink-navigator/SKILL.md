---
name: pharmalink-navigator
description: >-
  Orient and navigate the PharmaLink repository (Hyperledger Fabric 2.5 chaincode, Spring Boot 3.5
  backend, MongoDB CQRS cache, IPFS storage, Vaadin 24 UI) using Nix (flake.nix) and just (justfile).
  Use when onboarding into PharmaLink, locating modules, running builds/checks, or understanding
  dual-JDK and script pitfalls. Triggers: pharmalink, orient, repo layout, just check, nix develop,
  chaincode, vaadin. NOT: global Erynoa Surface authoring, production deployment, or unrelated repos.
---

# pharmalink-navigator — Codebase & DX Guide

## Repository Scope

PharmaLink is a completed hands-on demo built alongside a Bachelor's thesis (`Manufacturer → Wholesaler → Pharmacy → Authority`). Treat it as an evaluation and architectural reference, not a production-hardened deployment.

## Canonical Entry Points

| Concern | Location | What Lives There |
| :--- | :--- | :--- |
| **DX & Commands** | [`flake.nix`](../../../flake.nix) · [`justfile`](../../../justfile) | Reproducible `JDK 21` + `JDK 11` shell and all build/check/stack recipes |
| **Smart Contract** | [`PharmaSupplyChainContract.java`](../../../chaincode/pharmalink_chaincode_main/src/main/java/de/jklein/fabric/PharmaSupplyChainContract.java) | 30 `@Transaction` methods, X.509 `role` RBAC, 14 emitted events, 13 `PharmaSupplyChainErrors` |
| **CQRS & Sync** | [`SystemStateService.java`](../../../src/main/java/de/jklein/pharmalink/service/state/SystemStateService.java) | Block-checkpointed Fabric event listener updating `pharmalink.*` & `audit.chaincode_events` |
| **Fabric & IPFS** | [`FabricClient.java`](../../../src/main/java/de/jklein/pharmalink/client/fabric/FabricClient.java) · [`IpfsClient.java`](../../../src/main/java/de/jklein/pharmalink/client/ipfs/IpfsClient.java) | gRPC Gateway + `GrpcTransaction` audit; Kubo `/api/v0` + MongoDB cache + 5m negative cache |
| **Security & API** | [`SecurityConfig.java`](../../../src/main/java/de/jklein/pharmalink/config/SecurityConfig.java) · [`api-reference.md`](../../../docs/api-reference.md) | Dual FilterChains: stateless JWT `/api/**` (`@Order 1`) + form login `/app/**` (`@Order 2`) |
| **Vaadin 24 UI** | [`MainLayout.java`](../../../frontend/src/main/java/de/jklein/pharmalinkclient/views/MainLayout.java) | Views: `DashboardView` (ZXing QR), `MedikamenteView`, `UnitsView` (TreeGrid), `ActorExplorerView` |
| **Diagnostics UI** | [`AppController.java`](../../../src/main/java/de/jklein/pharmalink/controller/AppController.java) | Thymeleaf node dashboard at `/app/dashboard` showing raw audit JSON & KPIs |

## Verified Commands (`nix develop -c just ...`)

| Recipe | Purpose |
| :--- | :--- |
| `nix develop -c just doctor` | Check `JDK21_HOME`, `JDK11_HOME`, `just`, `jq`, `bash`, `python3`, `ruby`, and required files |
| `nix develop -c just build` | Compile backend (`JDK 21`), Vaadin frontend (`JDK 21`), and chaincode + Checkstyle (`JDK 11`) |
| `nix develop -c just check` | Full verification: `build` + `test` + `lint-scripts` + `check-configs` + `check-docs` |
| `nix develop -c just build-chaincode-dist` | Build chaincode distribution (`-x jacocoTestCoverageVerification`) |
| `nix develop -c just dev-infra-up` | Start dev MongoDB + IPFS via `src/main/dev/docker-compose.yaml` |
| `nix develop -c just up` / `just down` | Boot or safely tear down full Fabric test-network + Compose stack |

## Repo-Specific Pitfalls & Invariants

1. **Dual-JDK Toolchain:** Backend and `frontend/` target **JDK 21**; `chaincode/pharmalink_chaincode_main` pins **JDK 11** (`JavaLanguageVersion.of(11)`). `flake.nix` exports `JDK21_HOME`, `JDK11_HOME`, and `GRADLE_OPTS=-Dorg.gradle.java.installations.fromEnv=JDK21_HOME,JDK11_HOME` so `./gradlew` discovers both automatically.
2. **JaCoCo 90% Gate in Chaincode:** `chaincode/pharmalink_chaincode_main/build.gradle` binds `jacocoTestCoverageVerification` (`minimum = 0.9`) to `check` and `installDist` with zero unit tests present. Pass `-x jacocoTestCoverageVerification` (as `just build-chaincode-dist`, `scripts/fabric_setup_test.sh`, and `.github/workflows/release.yml` do) until contract tests exist.
3. **Role Scripts (`source` vs `bash`):** `scripts/roles/fabric_role_*.sh` switch CLI MSP env vars quietly when `source`d in `bash` or `zsh`, and execute `initCall` only when invoked directly via `bash`.
4. **Empty Static JS Stubs:** `src/main/resources/static/js/dashboard.js` and `main.js` are intentional 0-byte placeholders referenced by Thymeleaf layouts (`overview.html`, `default-layout.html`); keep them unless updating the templates.
5. **Test Stub & Known Quirks:** `PharmalinkApplicationTests` contains zero `@Test` methods; 5 Chaincode read queries omit `intent = Transaction.TYPE.EVALUATE`; `SecurityConfig.corsFilter()` pairs `allowCredentials(true)` with `"*"` origins.

## Do NOT

- Do NOT hardcode machine-local home paths or private hostnames in configs, scripts, or docs.
- Do NOT run `scripts/manage/repo_reset.sh --all-docker` casually; default `just down` tears down only PharmaLink resources.
- Do NOT delete [`evaluation/`](../../../evaluation/README.md) archives (`blöcke.zip`, `organizations.zip`, `openapi.json`).
- Do NOT deploy default test credentials (`admin`/`admin123`, `user`/`user`, dev JWT key) outside local test environments; see [`SECURITY.md`](../../../SECURITY.md).
