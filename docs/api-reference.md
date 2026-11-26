# REST API Reference

The Spring Boot backend exposes **28 paths (31 operations)** documented in OpenAPI 3 format.

- **Committed snapshot:** [`evaluation/openapi/openapi.json`](../evaluation/openapi/openapi.json)
- **Live Swagger UI:** `http://localhost:8080/api/swagger` (or `/swagger` depending on active config)
- **Live OpenAPI JSON:** `http://localhost:8080/api/api-docs`

## Authentication Model

- **API routes (`/api/**`)** use stateless Bearer JWT authentication (`SecurityConfig` FilterChain `@Order(1)`).
- Only `/api/v1/auth/**` is public; all other `/api/**` routes require `Authorization: Bearer <jwt>`.
- Every `/api/v1/**` request is recorded in MongoDB (`audit.api_transactions`) by `ApiTransactionInterceptor`.

---

## Endpoint Catalog

### Authentication (`AuthController`)

| Method | Path | Parameters / Body | Purpose |
| :--- | :--- | :--- | :--- |
| `POST` | `/api/v1/auth/login` | Body: `{ "username", "password" }` | Authenticate against MongoDB user store and issue HMAC JWT |

### Actors (`ActorFabricController`)

| Method | Path | Parameters / Body | Purpose |
| :--- | :--- | :--- | :--- |
| `GET` | `/api/v1/actors` | Query: `role` (required) | Query Fabric ledger for actors matching role (IPFS-enriched) |
| `PUT` | `/api/v1/actors/{actorId}` | Path: `actorId`; Body: `{ "name", "email", "ipfsData" }` | Update actor metadata and pin new IPFS profile JSON |

### Manufacturer Shortcuts (`HerstellerFabricController`)

| Method | Path | Parameters / Body | Purpose |
| :--- | :--- | :--- | :--- |
| `GET` | `/api/v1/hersteller/id` | — | Return the node's initialized `actorId` |
| `GET` | `/api/v1/hersteller/me` | — | Fetch IPFS-enriched `ActorResponseDto` for current node identity |
| `GET` | `/api/v1/hersteller/{herstellerId}` | Path: `herstellerId` | Fetch IPFS-enriched manufacturer profile by ID |
| `GET` | `/api/v1/hersteller/{herstellerId}/medications` | Path: `herstellerId` | Query ledger for all medications created by `herstellerId` |

### Medications (`MedicationFabricController`)

| Method | Path | Parameters / Body | Purpose |
| :--- | :--- | :--- | :--- |
| `GET` | `/api/v1/medications` | — | Query ledger for medications owned by current node actor |
| `POST` | `/api/v1/medications` | Body: `{ "bezeichnung", "infoblattHash", "ipfsData" }` | Pin `ipfsData` (if present) and submit `createMedikament` |
| `GET` | `/api/v1/medications/{medId}` | Path: `medId` | Query single medication by ID (IPFS-enriched) |
| `PUT` | `/api/v1/medications/{medId}` | Path: `medId`; Body: `{ "bezeichnung", "infoblattHash", "ipfsData" }` | Update medication and upload updated IPFS metadata |
| `POST` | `/api/v1/medications/{medId}/approval` | Path: `medId`; Body: `{ "newStatus": "freigegeben" \| "abgelehnt" }` | Regulatory approval/rejection (`behoerde` role required) |
| `GET` | `/api/v1/medications/search` | Query: `search` (required) | Legacy direct ledger regex search by name (prefer `/api/v1/search/medikamente`) |
| `DELETE` | `/api/v1/medications/{medId}/conditional-delete` | Path: `medId` | Submit `deleteMedikamentIfNoUnits` (`409 Conflict` if units exist) |

### Units & Cold Chain (`UnitFabricController`)

| Method | Path | Parameters / Body | Purpose |
| :--- | :--- | :--- | :--- |
| `POST` | `/api/v1/units/{medId}/units` | Path: `medId`; Body: `{ "chargeBezeichnung", "anzahl", "ipfsData" }` | Mint `anzahl` serialized units for an approved medication |
| `GET` | `/api/v1/units/{unitId}` | Path: `unitId` | Fetch single IPFS-enriched unit with full history |
| `GET` | `/api/v1/units/mine` | — | Query ledger for all units currently owned by active node actor |
| `GET` | `/api/v1/units/{medId}/units-by-charge` | Path: `medId` | Group all ledger units of a medication by `chargeBezeichnung` |
| `GET` | `/api/v1/units/medications/{medId}/charge-counts` | Path: `medId` | Return unit counts per batch (`Map<String, Integer>`) |
| `POST` | `/api/v1/units/{unitId}/transfer` | Path: `unitId`; Body: `{ "newOwnerActorId" }` | Transfer single unit custody to another registered actor |
| `POST` | `/api/v1/units/transfer-range` | Body: `{ "medId", "chargeBezeichnung", "startCounter", "endCounter", "newOwnerId", "transferTimestamp" }` | Bulk transfer contiguous serial range within a batch |
| `POST` | `/api/v1/units/{unitId}/temperature-readings` | Path: `unitId`; Body: `{ "temperature", "timestamp" }` | Append cold-chain reading to unit ledger record |
| `DELETE` | `/api/v1/units/{unitId}` | Path: `unitId` | Delete single unit (owner only) |
| `DELETE` | `/api/v1/units/batch` | Body: `{ "unitIds": [...] }` | Atomically delete multiple units owned by caller |

### CQRS Cached Search (`SearchController` — MongoDB Read-Model)

| Method | Path | Parameters | Purpose |
| :--- | :--- | :--- | :--- |
| `GET` | `/api/v1/search/actors` | `role`, `bezeichnung`, `actorId` (all optional) | Filter cached actors in MongoDB (`pharmalink.actors`) |
| `GET` | `/api/v1/search/medikamente` | `query`, `status`, `tags`, `ownedByMe` (all optional) | Multi-criteria search on cached medications (`pharmalink.medikamente`) |
| `GET` | `/api/v1/search/units` | `query` (required) | Search active node's cached units by `chargeBezeichnung` |
| `GET` | `/api/v1/search/my-units` | — | Return all cached units owned by active node actor |

### System & Cache Introspection (`SystemController`)

| Method | Path | Parameters | Purpose |
| :--- | :--- | :--- | :--- |
| `GET` | `/api/system/current-actor-id` | — | Return initialized Fabric `actorId` (or `204 No Content`) |
| `GET` | `/api/system/cache/stats` | — | Return counts (`actorCount`, `medikamentCount`, `myUnitsCount`) |
| `GET` | `/api/system/cache/state` | — | Return full CQRS snapshot (`SystemStateDto`) used by Vaadin `StateService` |

---

## End-to-End `curl` Walkthrough

### 1. Authenticate and Extract JWT

```bash
TOKEN=$(curl -s -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"admin123"}' | jq -r '.jwt')
```

### 2. Inspect Active Node Actor & Cache Stats

```bash
curl -s -H "Authorization: Bearer $TOKEN" \
  http://localhost:8080/api/system/cache/stats | jq
```

### 3. Register a Medication with IPFS Metadata (Manufacturer Node)

```bash
curl -s -X POST http://localhost:8080/api/v1/medications \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "bezeichnung": "Ibuprofen 400",
    "infoblattHash": "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855",
    "ipfsData": {
      "Dosage Form": "Film-coated tablets",
      "Package Size": "50 pcs"
    }
  }' | jq
```

### 4. Approve Medication (Authority Node)

```bash
MED_ID="<medId-from-step-3>"
curl -s -X POST "http://localhost:8080/api/v1/medications/${MED_ID}/approval" \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"newStatus": "freigegeben"}' | jq
```

### 5. Mint a Batch of Units & Append Temperature Reading

```bash
curl -s -X POST "http://localhost:8080/api/v1/units/${MED_ID}/units" \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"chargeBezeichnung": "CH-2025-07", "anzahl": 10}' | jq

UNIT_ID="${MED_ID}-CH-2025-07-0001"
curl -s -X POST "http://localhost:8080/api/v1/units/${UNIT_ID}/temperature-readings" \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"temperature": "5.4", "timestamp": "2025-07-12T10:15:00Z"}' | jq
```
