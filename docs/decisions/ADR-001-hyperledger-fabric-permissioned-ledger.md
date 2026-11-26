# ADR-001: Hyperledger Fabric 2.5 with X.509 Role Attributes

- **Status:** accepted
- **Date:** 2025-07-12 (documented retroactively 2026-11-26)

## Context

PharmaLink models a multi-party pharmaceutical supply chain across manufacturers (`hersteller`), wholesalers (`grosshaendler`), pharmacies (`apotheke`), and regulatory authorities (`behoerde`). Unlike open public cryptocurrencies, participants must be legally identifiable organizations with role-enforced write permissions and deterministic finality without gas fees or probabilistic forks.

## Decision

Adopt **Hyperledger Fabric 2.5** with:
1. **CouchDB StateDB** plus 7 Mango JSON indexes (`docType`, `actorId`, `role`, `email`, `herstellerId`, `bezeichnung`, `tags`) for rich ledger queries.
2. **X.509 Certificate (`ecert`) Attribute RBAC** embedded during Fabric CA enrollment (`role=<affiliation>:ecert`), read directly inside `PharmaSupplyChainContract` via `ctx.getClientIdentity().getAttributeValue("role")`.
3. **Deterministic Actor IDs** (`<role>-<sha256(mspId + "-" + clientId)>`) generated on-chain by `initCall`.

## Consequences

- **Positive:** Cryptographic proof of role without trust in off-chain API payloads; no transaction fee volatility; rich CouchDB query support alongside key-value lookups.
- **Negative:** Requires Fabric CA lifecycle management (`fabric-ca-client`), TLS certificate distribution, and Java 11 toolchain compatibility for the Fabric chaincode shim.
- **Residual Note:** Five read-only query methods in `PharmaSupplyChainContract` currently omit `intent = Transaction.TYPE.EVALUATE`; clients calling `evaluateTransaction()` still execute read-only, while CLI users should invoke read queries via `peer chaincode query`.
