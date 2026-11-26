# Documentation Index

Core documentation for **PharmaLink** follows a clean split between architecture, operator guides, domain specification, and decision logs.

## Guides & Reference

| Document | Scope |
| :--- | :--- |
| [Getting Started](./getting-started.md) | Prerequisites, full test-network startup, local IDE mode, env vars, teardown |
| [Architecture](./architecture.md) | Multi-country Fabric topology, CQRS MongoDB sync, IPFS caching, security, audit trail |
| [Domain Model](./domain-model.md) | On-chain data structures, medication & unit lifecycles, RBAC matrix, error codes |
| [API Reference](./api-reference.md) | All 28 REST endpoints (31 operations) with parameter contracts and `curl` workflow |

## Architecture Decisions (`docs/decisions/`)

| ADR | Decision |
| :--- | :--- |
| [ADR-001](./decisions/ADR-001-hyperledger-fabric-permissioned-ledger.md) | Hyperledger Fabric 2.5 with X.509 `role` attributes for permissioned supply-chain RBAC |
| [ADR-002](./decisions/ADR-002-offchain-ipfs-and-mongodb-read-cache.md) | IPFS off-chain document storage paired with event-driven MongoDB CQRS cache |
| [ADR-003](./decisions/ADR-003-dual-ui-vaadin-and-thymeleaf.md) | Dual web UI split: Vaadin 24 interactive client vs Thymeleaf backend diagnostics |

## Module & Artifact Docs

- [Agent Orientation & Repo Skill (`pharmalink-navigator`)](../.agents/README.md) · [`justfile`](../justfile) · [`flake.nix`](../flake.nix)
- [Chaincode Reference](../chaincode/README.md) & [Java Chaincode Module](../chaincode/pharmalink_chaincode_main/README.md)
- [Vaadin 24 Frontend Client](../frontend/README.md)
- [Evaluation Artifacts (Ledger Blocks, MSPs, OpenAPI)](../evaluation/README.md)
- [Architecture & UI Diagrams](./images) (`1_network-overview_countries.png` through `5_countries-detail.png`)
