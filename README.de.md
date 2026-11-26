# PharmaLink

> **Permissionierte Blockchain-Testplattform auf Basis von Hyperledger Fabric 2.5, Spring Boot 3.5, IPFS, MongoDB und Vaadin 24 zur lückenlosen Rückverfolgung von Medikamenten entlang der Lieferkette (`Hersteller → Großhändler → Apotheke → Behörde`).**

**Sprache:** [English](./README.md) | **Deutsch**

[![License: MIT](https://img.shields.io/badge/License-MIT-blue.svg)](./LICENSE)
![Status: Abgeschlossene Demo (Referenz)](https://img.shields.io/badge/Status-Abgeschlossene%20Demo%20%28Referenz%29-6c757d)
[![CI](https://github.com/NiklasJavier/PharmaLink/actions/workflows/ci.yml/badge.svg)](https://github.com/NiklasJavier/PharmaLink/actions/workflows/ci.yml)
[![Java](https://img.shields.io/badge/Java-21%20%28App%29%20%7C%2011%20%28Chaincode%29-ED8B00?logo=openjdk&logoColor=white)](./build.gradle)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.5-6DB33F?logo=springboot&logoColor=white)](./build.gradle)
[![Hyperledger Fabric](https://img.shields.io/badge/Hyperledger%20Fabric-2.5-2F3134?logo=hyperledger&logoColor=white)](./chaincode/README.md)
[![Vaadin](https://img.shields.io/badge/Vaadin-24.8-00B4F0?logo=vaadin&logoColor=white)](./frontend/README.md)
[![IPFS](https://img.shields.io/badge/IPFS-Kubo-65C2CB?logo=ipfs&logoColor=white)](./docker/docker-compose-pharmalink.yaml)
[![MongoDB](https://img.shields.io/badge/MongoDB-CQRS%20Read--Model-47A248?logo=mongodb&logoColor=white)](./docs/architecture.md)

---

> [!NOTE]
> **Projektkontext:** PharmaLink entstand als praxisnahe Demo begleitend zur Bachelorarbeit, um die Konzepte permissionierter Blockchains in der pharmazeutischen Lieferkette praktisch zu durchdringen. Das Projekt ist **abgeschlossen und wird nicht aktiv weiterentwickelt**; es bleibt als Referenz öffentlich zugänglich. Das Repository nutzt lokale Fabric-Testnetzwerk-CAs und Standard-Entwicklungszugangsdaten (siehe [`SECURITY.md`](./SECURITY.md)) — nicht unverändert produktiv einsetzen.

## Visuelle Übersicht

<table>
  <tr>
    <td align="center">
      <a href="./docs/images/1_network-overview_countries.png">
        <img src="./docs/images/1_network-overview_countries.png" width="380" alt="Föderiertes Länderverbund-Netzwerk">
      </a>
      <br/><sub><b>Föderiertes Kanal-Netzwerk (<code>Länderverbund</code>)</b></sub>
    </td>
    <td align="center">
      <a href="./docs/images/2_country-architecture_local.png">
        <img src="./docs/images/2_country-architecture_local.png" width="380" alt="Lokale Architektur eines Landes">
      </a>
      <br/><sub><b>Lokale Landesarchitektur (Peers, CA, IPFS, Backend, MongoDB, UI)</b></sub>
    </td>
  </tr>
  <tr>
    <td align="center">
      <a href="./docs/images/3_ui-medications.png">
        <img src="./docs/images/3_ui-medications.png" width="380" alt="Vaadin 24 Client UI Medikamente">
      </a>
      <br/><sub><b>Vaadin 24 Client UI — Medikamentendetails, IPFS-Daten &amp; Freigabe</b></sub>
    </td>
    <td align="center">
      <a href="./docs/images/4_transaction-sequence_med-create.png">
        <img src="./docs/images/4_transaction-sequence_med-create.png" width="380" alt="Transaktionssequenz Medikament erstellen">
      </a>
      <br/><sub><b>Transaktionsablauf — Medikament anlegen &amp; IPFS-Speicherung</b></sub>
    </td>
  </tr>
</table>

## Architektur- und Funktionshighlights

- **X.509-Zertifikat-basierte RBAC:** Jede Aufrufer-Identität leitet sich deterministisch aus MSP-ID, Client-Zertifikat und dem `role`-Attribut (`role-<sha256(mspId-clientId)>`) ab und wird direkt im Java-Chaincode über 30 `@Transaction`-Funktionen erzwungen.
- **Event-gesteuertes CQRS-Read-Model:** `SystemStateService` sichert den zuletzt verarbeiteten Block als Checkpoint in MongoDB und abonniert alle 14 Chaincode-Events per gRPC. Abfragen bleiben schnell, ohne bei jedem Read den Peer abzufragen.
- **IPFS Off-Chain-Speicher & Dual-Cache:** Erweiterte Beipackzettel-, Chargen- und Akteursdaten liegen in IPFS (`IpfsClient`), ergänzt um SHA-256-Hash-Prüfung, MongoDB-Positiv-Cache (`IpfsCacheEntry`) und einen 5-Minuten-Negativ-Cache.
- **Kühlketten- & Bereichs-Tracking:** Hersteller legen Medikamente an (`angelegt`), Behörden geben frei oder lehnen ab (`freigegeben` / `abgelehnt`), und Eigentümer erstellen nummerierte Einheiten, erfassen Temperaturmesswerte oder übertragen ganze Serienbereiche (`transferUnitRange`).
- **4-Schicht-Audit-Trail:** Eigene MongoDB-Collections protokollieren HTTP-API-Aufrufe (`ApiTransaction`), Fabric-gRPC-Transaktionen (`GrpcTransaction`), Chaincode-Events (`ChaincodeEventLog`) und Login-Versuche (`LoginAttempt`).
- **Duale Weboberflächen & OpenAPI:** Rollenbasierte Vaadin-24-Oberfläche (inkl. ZXing-QR-Codes für Akteur-IDs und Light/Dark-Theme-Wechsel), eingebettetes Thymeleaf-Diagnose-Dashboard sowie eine OpenAPI-Spezifikation mit 28 Pfaden / 31 Operationen.

## Systemarchitektur im Überblick

```mermaid
flowchart LR
    UI["Vaadin 24 Client UI<br/>(Port 3000 / 8083)"] -->|REST + JWT| API["Spring Boot Backend<br/>(Port 8080)"]
    API -->|Thymeleaf| Diag["Eingebettetes Node-<br/>Diagnose-Dashboard"]
    API -->|gRPC / Fabric Gateway| Peer["Hyperledger Fabric Peer<br/>(Channel: pharmalink)"]
    Peer --> Chaincode["Java Chaincode + CouchDB<br/>(30 Tx · 7 Indizes · 14 Events)"]
    API -->|HTTP /api/v0| IPFS["IPFS Kubo Node<br/>(Port 5001 / 8081)"]
    Peer -.->|Chaincode-Events| API
    API -->|CQRS-Cache & Audit| Mongo[("MongoDB<br/>(Port 27017)")]
```

| Komponente | Pfad | Technologie | Verantwortung |
| :--- | :--- | :--- | :--- |
| **Backend-API & Sync** | [`./src`](./src) | Java 21, Spring Boot 3.5, Fabric Gateway, MongoDB, Thymeleaf | REST-API (`/api/v1/**`), JWT- & Form-Auth, IPFS-Cache, Block-Sync, 4-Schicht-Audit |
| **Client-Frontend** | [`./frontend`](./frontend) | Java 21, Vaadin 24.8, WebFlux, ZXing | Interaktive Web-UI (Dashboard + QR-Codes, Akteur-Explorer, Medikamente, Unit-TreeGrid) |
| **Smart Contract** | [`./chaincode/pharmalink_chaincode_main`](./chaincode/pharmalink_chaincode_main) | Java 11, Hyperledger Fabric Chaincode Shim 2.5, Genson | On-Chain-Geschäftslogik, X.509-RBAC, CouchDB-Indizes, Event-Auslösung |
| **Netzwerk & Container** | [`./scripts`](./scripts), [`./docker`](./docker), [`./common`](./common) | Bash, Docker Compose, Fabric 2.5.13, CouchDB, Kubo | Lokales Multi-Org-Fabric-Testnetzwerk, CA-Konsortium, Stack-Orchestrierung |

## Akteure & Rollen im Netzwerk

| Rolle (`role`-Attribut) | Zuständigkeit | Kernfunktionen |
| :--- | :--- | :--- |
| `hersteller` | Hersteller | Medikamente anlegen und aktualisieren, eigene Tags setzen, Einheiten freigegebener Medikamente erzeugen und übertragen |
| `behoerde` | Regulierungsbehörde | Medikamente freigeben oder ablehnen (`freigegeben` / `abgelehnt`), Prüftags setzen, Akteure und Medikamente verwalten |
| `grosshaendler` | Großhändler | Einheiten im Bestand führen, Temperaturmesswerte erfassen, Einzel- und Bereichstransfers durchführen |
| `apotheke` | Apotheke | Einheiten entgegennehmen und prüfen, Temperaturhistorie ergänzen, lückenlose Herkunft und Infoblatt-Hash nachvollziehen |

## Schnellstart (Lokales Testnetzwerk)

**Voraussetzungen:** Docker + Docker Compose v2, Java 11 & Java 21 (z. B. über [SDKMAN](https://sdkman.io/)), `curl`, `git`, `jq` und `unzip`. Details zu IDE-Entwicklung und Umgebungsvariablen stehen in [docs/getting-started.md](./docs/getting-started.md).

```bash
# 1. Repository klonen
git clone https://github.com/NiklasJavier/PharmaLink.git
cd PharmaLink

# 2. Fabric-Umgebungsvariablen laden (Pfad wird dynamisch erkannt)
source ./scripts/fabric_setEnv.sh

# 3. Hyperledger Fabric Test-Netzwerk starten (Channel: pharmalink, StateDB: CouchDB)
./scripts/fabric_setup_test.sh up

# 4. Java-Chaincode bauen (JDK 11) und deployen
./scripts/fabric_setup_test.sh cc

# 5. Konsortiums-Identitäten (3 Nutzer je Rolle) anlegen und initCall ausführen
./scripts/fabric_setup_test_consortium.sh

# 6. Backend, Vaadin-UI, IPFS und MongoDB per Docker Compose starten
./scripts/pharmalink_setup.sh up
```

### Standard-Endpunkte

| Dienst | Endpunkt | Hinweis |
| :--- | :--- | :--- |
| **Vaadin Client UI** | `http://localhost:3000` | Primäre Weboberfläche (`admin` / `admin123`) |
| **Backend REST-API** | `http://localhost:8080/api/v1` | JWT-geschützte REST-API (`/api/v1/auth/login` offen) |
| **Swagger UI / OpenAPI** | `http://localhost:8080/api/swagger` | Interaktive API-Doku (Schema unter `/api/api-docs` bzw. [`evaluation/openapi/openapi.json`](./evaluation/openapi/openapi.json)) |
| **Backend Diagnose-UI** | `http://localhost:8080/app/dashboard` | Eingebettetes Thymeleaf-Audit- & State-Dashboard (`admin` / `admin123`) |
| **IPFS RPC / Gateway** | `http://localhost:5001` / `http://localhost:8081` | Off-Chain-Speicher (`ipfs/kubo:v0.29.0`) |
| **MongoDB** | `mongodb://localhost:27017` | CQRS-Read-Model, IPFS-Cache & Audit-Collections (`user` / `user`) |
| **Fabric Peer / Orderer / CA** | `localhost:7051` (`Org1`), `9051` (`Org2`) / `7050` / `7054` | Lokale Fabric-Testnetzwerk-Endpunkte |

### CLI-Rolle wechseln & Umgebung zurücksetzen

```bash
# Identität im aktuellen Terminal setzen (löst beim Sourcen kein initCall aus)
source ./scripts/roles/fabric_role_behoerde.sh

# PharmaLink-Compose-Stack und Fabric-Testnetzwerk sauber beenden
./scripts/manage/repo_reset.sh
```

## Dokumentation

| Dokument | Inhalt |
| :--- | :--- |
| [docs/README.md](./docs/README.md) | Navigationsübersicht aller technischen Dokumente |
| [docs/getting-started.md](./docs/getting-started.md) | Installation, lokaler Dev-Modus, Konfigurationsvariablen, Fehlerbehebung |
| [docs/architecture.md](./docs/architecture.md) | Föderierte Netzwerktopologie, CQRS-Sync, IPFS-Caching, Sicherheit & 4-Schicht-Audit |
| [docs/domain-model.md](./docs/domain-model.md) | Datenmodell, Zustandsautomaten, RBAC-Berechtigungsmatrix, 13 Fehlercodes |
| [docs/api-reference.md](./docs/api-reference.md) | Referenz aller 28 REST-Pfade (31 Operationen) inkl. `curl`-Workflow |
| [docs/decisions/](./docs/decisions) | Architecture Decision Records (ADR-001 bis ADR-003) |
| [chaincode/README.md](./chaincode/README.md) | Smart-Contract-Spezifikation: 30 Transaktionen, 7 CouchDB-Indizes, 14 Events |
| [frontend/README.md](./frontend/README.md) | Vaadin-24-Client: Views, State-Service, QR-Code-Generierung & Theming |
| [evaluation/README.md](./evaluation/README.md) | Evaluationsartefakte (`openapi.json`, `blöcke.zip`, `organizations.zip`) |

## Projektstatus, Sicherheit & Lizenz

- **Status:** Abgeschlossene Praxis-Demo aus dem Umfeld der Bachelorarbeit ohne aktive Weiterentwicklung; Hinweise für eigene Forks und lokale Build-Checks stehen in [CONTRIBUTING.md](./CONTRIBUTING.md).
- **Sicherheit:** Abgrenzung der Test-Credentials und Härtungshinweise stehen in [SECURITY.md](./SECURITY.md).
- **Lizenz:** Veröffentlicht unter der [MIT-Lizenz](./LICENSE).
