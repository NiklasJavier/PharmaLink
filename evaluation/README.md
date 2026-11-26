# Evaluation Artifacts

This directory preserves captured snapshots from test-network evaluation runs of PharmaLink.

| Artifact | Size / Contents | Purpose |
| :--- | :--- | :--- |
| [`openapi/openapi.json`](./openapi/openapi.json) | 28 paths · 31 HTTP operations | Captured OpenAPI 3 specification from SpringDoc |
| [`blöcke.zip`](./blöcke.zip) | 49 JSON files (`all_blocks.json` + `json_blocks/block_0.json`–`block_47.json`) | Decoded Hyperledger Fabric ledger block snapshots from test transactions |
| [`msp/organizations.zip`](./msp/organizations.zip) | 270 files | Captured MSP and TLS certificate tree from a local test-network evaluation run |

> [!WARNING]
> **Test-Network Artifacts Only:** `msp/organizations.zip` contains private keys and certificates generated solely for local Hyperledger Fabric test-network (`example.com`) evaluation. Never reuse any certificate or key material from this archive outside ephemeral local tests.

## Inspecting Captured Ledger Blocks

```bash
unzip -l evaluation/blöcke.zip
unzip -p evaluation/blöcke.zip json_blocks/block_10.json | jq .
```
