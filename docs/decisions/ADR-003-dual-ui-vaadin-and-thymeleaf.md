# ADR-003: Dual Web UI Split (Vaadin 24 Client & Embedded Thymeleaf Diagnostics)

- **Status:** accepted
- **Date:** 2025-08-10 (documented retroactively 2026-11-26)

## Context

Each supply-chain participant runs a local backend node connected to Fabric, IPFS, and MongoDB, and also needs an end-user web client for day-to-day operations (medication approval, unit minting, batch transfers, actor lookup, QR-code export). Building everything into server-rendered templates inside the backend couples UI ergonomics to backend deployments, while putting raw node audit streams in the business UI clutters domain workflows.

## Decision

Maintain two intentionally separated UI surfaces:
1. **Standalone Vaadin 24 Client (`./frontend`, port `8083` / Compose `3000`):** Role-aware business UI (`DashboardView` with ZXing QR codes, `ActorExplorerView`, `MedikamenteView`, `UnitsView`) communicating solely with the backend REST API using stateless Bearer JWTs (`BACKEND_BASEURL`).
2. **Embedded Thymeleaf Node Dashboard (`./src/main/resources/web/`, port `8080`, `/app/dashboard`):** Lightweight server-rendered operator diagnostics page on the backend node itself, displaying raw `ApiTransaction`, `GrpcTransaction`, and `LoginAttempt` audit logs alongside cached collections.

## Consequences

- **Positive:** Clean API boundary between client UI and node backend; independent scaling/deployment of `pharmalink-ui` (`frontend/Dockerfile`); zero raw audit-log clutter in the supply-chain workflow UI.
- **Negative:** Two Spring Boot builds and two independent login screens (`/login` on Vaadin vs `/app/login` on backend) to maintain.
