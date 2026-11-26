# Chaincode Reference (`pharmalink_chaincode_main`)

**Roles:** `behoerde`, `hersteller`, `grosshaendler`, `apotheke`  
**Authorization:** Role-Based Access Control (RBAC) via X.509 certificate attributes (`role=<role>:ecert`) evaluated directly from `Context.getClientIdentity()`.

Consortium certificates and test identities are provisioned by [`scripts/fabric_setup_test_consortium.sh`](../scripts/fabric_setup_test_consortium.sh). CLI environment variables for any role can be loaded directly from [`scripts/roles/`](../scripts/roles) via `source`:

```bash
source ./scripts/roles/fabric_role_hersteller.sh
```

Full implementation: [`PharmaSupplyChainContract.java`](./pharmalink_chaincode_main/src/main/java/de/jklein/fabric/PharmaSupplyChainContract.java).

---

## 1. Contract Transactions (30)

All 30 `@Transaction` methods implemented in `PharmaSupplyChainContract`:

| Category | Function | Fabric Intent | Purpose & Authorization | Example Invocation (JSON) |
| :--- | :--- | :--- | :--- | :--- |
| **Actor** | `initCall` | `SUBMIT` | Derives deterministic `actorId = <role>-<sha256(mspId-clientId)>` from X.509 `role` attribute and registers minimal actor state (idempotent if already present). **Auth:** Valid `role` attribute (`hersteller`, `grosshaendler`, `apotheke`, `behoerde`). | `{"function":"initCall","Args":[]}` |
| | `createActor` | `SUBMIT` | Manually creates an actor record with full metadata. **Auth:** `behoerde` only. | `{"function":"createActor","Args":["apotheke-123","Sonnen-Apotheke","apotheke","info@sonnen-apotheke.de","Qm..."]}` |
| | `updateActor` | `SUBMIT` | Updates `bezeichnung`, `email`, and `ipfsLink` of an existing actor. **Auth:** Target actor itself or `behoerde`. | `{"function":"updateActor","Args":["apotheke-123","Sonnen-Apotheke Neu","new@sonnen-apotheke.de","QmNew..."]}` |
| | `updateActorIpfsLink` | `SUBMIT` | Updates only `ipfsLink` of an actor. **Auth:** Strictly the target actor itself. | `{"function":"updateActorIpfsLink","Args":["apotheke-123","QmUpdated..."]}` |
| | `deleteActor` | `SUBMIT` | Removes an actor record from World State. **Auth:** `behoerde` only. | `{"function":"deleteActor","Args":["apotheke-123"]}` |
| | `queryActor` | Default (`SUBMIT`*) | Fetches an actor JSON by `actorId`. **Auth:** Any enrolled channel member. | `{"function":"queryActor","Args":["apotheke-123"]}` |
| | `queryActorById` | `EVALUATE` | Evaluates and returns an actor JSON by `actorId`. **Auth:** Any enrolled member. | `{"function":"queryActorById","Args":["apotheke-123"]}` |
| | `queryActorByEmail` | `EVALUATE` | Queries the first actor matching `email` via CouchDB index `indexEmail`. **Auth:** Any enrolled member. | `{"function":"queryActorByEmail","Args":["info@sonnen-apotheke.de"]}` |
| | `queryActorsByRole` | `EVALUATE` | Queries all actors matching lowercase `role` via CouchDB index `indexRole`. **Auth:** Any enrolled member. | `{"function":"queryActorsByRole","Args":["apotheke"]}` |
| | `queryActorsByBezeichnung` | `EVALUATE` | Case-insensitive regex search on `bezeichnung` (`docType = "actor"`). **Auth:** Any enrolled member. | `{"function":"queryActorsByBezeichnung","Args":["Sonnen-Apotheke"]}` |
| | `queryAllActors` | Default (`SUBMIT`*) | Returns all `docType = "actor"` records via CouchDB index `indexDocType`. **Auth:** Any enrolled member. | `{"function":"queryAllActors","Args":[]}` |
| **Medikament** | `createMedikament` | `SUBMIT` | Creates a medication with `medId = MED-<sha256(herstellerId-bezeichnung)>`, `status = "angelegt"`, and initializes `unitCounter_<medId>` to `0`. **Auth:** `hersteller` only. | `{"function":"createMedikament","Args":["Aspirin 500mg","hash123","Qm..."]}` |
| | `approveMedikament` | `SUBMIT` | Sets medication status to `freigegeben` or `abgelehnt` and records `approvedById`. **Auth:** `behoerde` only. | `{"function":"approveMedikament","Args":["MED-abc...","freigegeben"]}` |
| | `updateMedikament` | `SUBMIT` | Updates non-empty `bezeichnung`, `infoblattHash`, and `ipfsLink`. **Auth:** Creating `hersteller` only. | `{"function":"updateMedikament","Args":["MED-abc...","Aspirin Forte","newhash","newQm..."]}` |
| | `addMedikamentTag` | `SUBMIT` | Sets a role-keyed tag (`tags["hersteller"]` or `tags["behoerde"]`). **Auth:** Creating `hersteller` or any `behoerde`. | `{"function":"addMedikamentTag","Args":["MED-abc...","Charge 2 geprüft"]}` |
| | `deleteMedikament` | `SUBMIT` | Deletes medication and its `unitCounter_<medId>` key unconditionally. **Auth:** `behoerde` (any status) or creating `hersteller` (only while `status = "angelegt"`). | `{"function":"deleteMedikament","Args":["MED-abc..."]}` |
| | `deleteMedikamentIfNoUnits` | `SUBMIT` | Deletes medication and counter only if zero units exist for `medId`. **Auth:** `behoerde` (any status) or creating `hersteller` (`status = "angelegt"`). | `{"function":"deleteMedikamentIfNoUnits","Args":["MED-abc..."]}` |
| | `queryMedikamentById` | `EVALUATE` | Fetches a medication record by `medId`. **Auth:** Any enrolled member. | `{"function":"queryMedikamentById","Args":["MED-abc..."]}` |
| | `queryMedikamenteByHerstellerId` | `EVALUATE` | Queries medications created by `herstellerId` via `indexHerstellerId`. **Auth:** Any enrolled member. | `{"function":"queryMedikamenteByHerstellerId","Args":["hersteller-xyz..."]}` |
| | `queryMedikamenteByBezeichnung` | `EVALUATE` | Case-insensitive regex search on medication `bezeichnung`. **Auth:** Any enrolled member. | `{"function":"queryMedikamenteByBezeichnung","Args":["Aspirin"]}` |
| | `queryAllMedikamente` | `EVALUATE` | Returns all `docType = "medikament"` records. **Auth:** Any enrolled member. | `{"function":"queryAllMedikamente","Args":[]}` |
| | `queryChargeCountsByMedId` | `EVALUATE` | Returns `Map<chargeBezeichnung, count>` of existing units for a medication. **Auth:** Any enrolled member. | `{"function":"queryChargeCountsByMedId","Args":["MED-abc..."]}` |
| **Unit** | `createUnits` | `SUBMIT` | Mints `anzahl` sequential units (`<medId>-<charge>-<04d>`) owned initially by the manufacturer. **Auth:** Creating `hersteller` only; medication must be `freigegeben`. | `{"function":"createUnits","Args":["MED-abc...","CH-2025-07","100","QmUnits..."]}` |
| | `addTemperatureReading` | `SUBMIT` | Appends `{timestamp, temperature}` to a unit's cold-chain history. **Auth:** Current unit owner (`currentOwnerActorId`) only. | `{"function":"addTemperatureReading","Args":["MED-abc...-CH-2025-07-0001","5.5","2025-07-12T10:00:00Z"]}` |
| | `transferUnit` | `SUBMIT` | Transfers a single unit to an existing actor and appends a `TransferEntry`. **Auth:** Current unit owner only. | `{"function":"transferUnit","Args":["MED-abc...-CH-2025-07-0001","apotheke-123","2025-07-12T11:00:00Z"]}` |
| | `transferUnitRange` | `SUBMIT` | Atomically transfers units numbered `startCounter..endCounter` within a batch to `newOwnerActorId`. **Auth:** Current owner of every unit in range. | `{"function":"transferUnitRange","Args":["MED-abc...","CH-2025-07","1","50","grosshaendler-456","2025-07-12T12:00:00Z"]}` |
| | `deleteUnits` | `SUBMIT` | Atomically deletes a JSON array of `unitIds`. **Auth:** Current owner of every listed unit. | `{"function":"deleteUnits","Args":["[\"UNIT-001\",\"UNIT-002\"]"]}` |
| | `queryUnitById` | Default (`SUBMIT`*) | Fetches a single unit by `unitId`. **Auth:** Any enrolled member. | `{"function":"queryUnitById","Args":["UNIT-xyz..."]}` |
| | `queryUnitsByMedId` | Default (`SUBMIT`*) | Queries all units associated with `medId`. **Auth:** Any enrolled member. | `{"function":"queryUnitsByMedId","Args":["MED-abc..."]}` |
| | `queryUnitsByOwner` | Default (`SUBMIT`*) | Queries all units currently owned by `ownerActorId`. **Auth:** Any enrolled member. | `{"function":"queryUnitsByOwner","Args":["hersteller-xyz..."]}` |

> \* **Note on read annotations:** Five read methods (`queryActor`, `queryAllActors`, `queryUnitById`, `queryUnitsByMedId`, `queryUnitsByOwner`) use bare `@Transaction()` without `intent = Transaction.TYPE.EVALUATE`. `FabricClient` invokes them via `evaluateTransaction()` so they still run as read-only evaluations; from the Fabric CLI, always call them with `peer chaincode query`.

---

## 2. CouchDB Mango Indexes (7)

Packaged under `pharmalink_chaincode_main/src/main/resources/META-INF/statedb/couchdb/indexes/`:

| File | Design Doc | Indexed Fields |
| :--- | :--- | :--- |
| `indexActorId.json` | `indexActorIdDoc` | `["actorId"]` |
| `indexDocType.json` | `indexDocTypeDoc` | `["docType"]` |
| `indexEmail.json` | `indexEmailDoc` | `["email", "docType"]` |
| `indexHerstellerId.json` | `indexHerstellerIdDoc` | `["herstellerId", "docType"]` |
| `indexMedBezeichnung.json` | `indexMedBezeichnungDoc` | `["bezeichnung", "docType"]` |
| `indexMedTags.json` | `indexMedTagsDoc` | `["tags", "docType"]` |
| `indexRole.json` | `indexRoleDoc` | `["role", "docType"]` |

---

## 3. Emitted Events (14)

Every state-changing transaction emits an event consumed by `SystemStateService`:

- **Actor:** `ActorInitialized`, `ActorCreated`, `ActorUpdated`, `ActorIpfsLinkUpdated`, `ActorDeleted`
- **Medikament:** `MedikamentCreated`, `MedikamentStatusUpdated`, `MedikamentUpdated`, `MedikamentTagAdded`, `MedikamentDeleted`
- **Unit:** `UnitCreated`, `UnitTemperatureAdded`, `UnitTransferred`, `UnitDeleted`

---

## 4. Interacting via Fabric CLI

```bash
# 1. Load identity & network environment variables without submitting initCall
source ./scripts/roles/fabric_role_behoerde.sh

# 2. Query all registered actors (read-only)
peer chaincode query \
  -C "$CHANNEL_NAME" -n "$CHAINCODE" \
  -c '{"function":"queryAllActors","Args":[]}'

# 3. Invoke a state-changing transaction (requires orderer + endorsing peers)
peer chaincode invoke \
  -o "$ORDERER_ADDRESS" --tls --cafile "$ORDERER_CA" \
  -C "$CHANNEL_NAME" -n "$CHAINCODE" \
  --peerAddresses "$PEER0_ORG1_ADDRESS" --tlsRootCertFiles "$PEER0_ORG1_CA" \
  --peerAddresses "$PEER0_ORG2_ADDRESS" --tlsRootCertFiles "$PEER0_ORG2_CA" \
  -c '{"function":"approveMedikament","Args":["MED-abc...","freigegeben"]}'
```
