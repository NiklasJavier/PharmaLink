# Changelog

All notable changes to **PharmaLink** are documented in this file.

The format follows [Keep a Changelog](https://keepachangelog.com/en/1.1.0/) and [Conventional Commits](https://www.conventionalcommits.org/).

## [Unreleased]

### Added
- Declarative Nix Flake dev shell (`flake.nix`, `flake.lock`, `.envrc`) provisioning both JDK 21 and JDK 11, paired with a `justfile` command surface (`just doctor`, `just check`, `just up`, `just down`, `just role`).
- Repository agent orientation under `.agents/` (`.agents/README.md` and `.agents/skills/pharmalink-navigator/SKILL.md`).
- Overhauled root `README.md` with Mermaid architecture diagram, verified visual gallery, and end-to-end verification quickstart.
- Structured documentation under `docs/`: `README.md`, `getting-started.md`, `architecture.md`, `domain-model.md`, `api-reference.md`, and ADRs (`ADR-001` through `ADR-003`).
- Module guides (`frontend/README.md`, `evaluation/README.md`) and rewritten `chaincode/pharmalink_chaincode_main/README.md`.
- Community & repository governance files (`CONTRIBUTING.md`, `SECURITY.md`, `CODE_OF_CONDUCT.md`, GitHub issue/PR templates).
- GitHub Actions CI workflow (`.github/workflows/ci.yml`) for backend, frontend, chaincode, shell syntax, JSON, and YAML checks.

### Changed
- Sanitized hardcoded local user paths, private test hostnames, and stale Quarkus `.gitignore` entries in favor of environment-parameterized defaults.
- Updated `docker/docker-compose-pharmalink.yaml` to drop the obsolete `version` key, remove an unused `/app/config` volume mount, and use `ipfs/kubo:v0.29.0`.
- Reworked all setup, role, and reset scripts in `scripts/` for dynamic repo-root path resolution, Linux/macOS + `bash`/`zsh` compatibility, Compose v2 auto-detection, and safe non-destructive reset defaults (`--all-docker` opt-in).
- Corrected `chaincode/pharmalink_chaincode_main/Dockerfile` group creation flag and JaCoCo build flag; refined `.github/workflows/release.yml` to build both backend and frontend GHCR images.
- Updated `chaincode/README.md` to match all 30 contract transactions, 7 CouchDB indexes, and 14 emitted events.

### Removed
- Unreferenced empty placeholder files (`frontend/src/main/resources/application.yaml`, `src/main/resources/web/auth/profile.html`) and unused upstream `chaincode/templates/` sample boilerplate.

## [2025-09-14] — Initial Public Snapshot

### Added
- Spring Boot 3.5 backend with Hyperledger Fabric Gateway integration, event-driven MongoDB CQRS sync, IPFS client + cache, 4-layer audit trail, JWT + Thymeleaf diagnostics UI, and OpenAPI spec (`794f695`).
- Vaadin 24.8 frontend client with Dashboard (ZXing QR codes), Actor Explorer, Medications workflow, and Unit TreeGrid.
- Java 11 Hyperledger Fabric chaincode (`PharmaSupplyChainContract`) with X.509 `role` RBAC and CouchDB indexes.
- MIT License (`a9e0f37`) and evaluation block/MSP/OpenAPI artifacts.
