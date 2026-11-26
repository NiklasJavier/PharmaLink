# ADR-002: Off-Chain IPFS Storage and Event-Driven MongoDB CQRS Read-Model

- **Status:** accepted
- **Date:** 2025-07-15 (documented retroactively 2026-11-26)

## Context

Pharmaceutical records frequently include rich semi-structured metadata (dosage forms, packaging details, extended actor profiles, batch attachments). Writing large arbitrary JSON blobs directly onto the Fabric ledger inflates block sizes and chaincode state payloads, while running complex filtered search queries directly against CouchDB over gRPC slows down interactive UI filtering.

## Decision

Separate **authoritative state**, **bulk metadata**, and **query projections**:
1. **IPFS (`ipfs/kubo` + `IpfsClient`):** Store extended `ipfsData` payloads off-chain and record only the content-addressed Base58 CID (`ipfsLink`) and leaflet SHA-256 (`infoblattHash`) on the Fabric ledger.
2. **MongoDB Positive & Negative IPFS Cache (`IpfsCacheEntry` + `failedHashesCache`):** Cache successfully fetched CIDs in `pharmalink.ipfs_cache` and apply a 5-minute in-memory negative cooldown for unreachable CIDs.
3. **Event-Driven MongoDB CQRS Projection (`SystemStateService`):** Listen to all 14 chaincode events (`Actor*`, `Medikament*`, `Unit*`) with persisted `lastProcessedBlockNumber` checkpoints in `pharmalink.state`, serving `/api/v1/search/**` and `/api/system/cache/**` directly from MongoDB.

## Consequences

- **Positive:** Compact on-chain blocks; sub-second UI search across actors, medications, and units; resilient catch-up across restarts via block checkpointing; automatic auditability via `audit.chaincode_events`.
- **Negative:** Eventual consistency window between ledger commit and MongoDB event projection; operational dependency on both MongoDB and IPFS alongside Fabric.
- **Mitigation:** Endpoints under `/api/v1/medications/{id}` and `/api/v1/units/{id}` still support direct ledger reads when strict ledger freshness is required.
