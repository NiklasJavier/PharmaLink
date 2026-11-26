# PharmaLink Client UI (`frontend`)

Standalone **Vaadin 24.8** + **Spring Boot 3.5** web interface for supply-chain participants (`Manufacturer`, `Wholesaler`, `Pharmacy`, `Regulatory Authority`).

## Features & Views

| Route | Class | Purpose |
| :--- | :--- | :--- |
| `/login` | `LoginView` | Authenticates via `POST /api/v1/auth/login` and establishes Vaadin session security context |
| `/` | `DashboardView` | Displays active username/role, CQRS cache stats, click-to-copy fields, and a **ZXing** QR code for `actorId` |
| `/medikamente` | `MedikamenteView` | Browse/filter medications (`No Filter`, `angelegt`, `freigegeben`, `abgelehnt`), inspect IPFS details & manufacturer info, approve/reject status, create units |
| `/units` | `UnitsView` | 3-level `TreeGrid` (`Medikament → Charge → Unit`) with "My Units" & "Global Search" tabs, temperature logs, and custody transfer actions |
| `/actorExplorer` | `ActorExplorerView` | Master-detail explorer of supply-chain actors (`Manufacturer`, `Wholesaler`, `Pharmacy`, `Authority`) with search and profile updates |

`MainLayout` includes a two-row navigation header, session menu, and a persistent **Light / Dark Lumo theme toggle**.

## Configuration

| Property / Env Var | Default | Purpose |
| :--- | :--- | :--- |
| `PORT` (`server.port`) | `8083` (Compose maps `3000:8083`) | HTTP listen port |
| `BACKEND_BASEURL` (`backend.baseUrl`) | `http://localhost:8080/api` (`http://app-pharmalink:8080/api` in Compose) | Base URL of the PharmaLink Spring Boot backend |

## Build & Run

Run commands from the repository root (`PharmaLink/`) with **JDK 21**:

```bash
# Compile Vaadin client
./gradlew -p frontend compileJava

# Run locally in dev mode (expects backend on http://localhost:8080/api)
./gradlew -p frontend bootRun

# Build production Spring Boot JAR + Vaadin production bundle
./gradlew -p frontend bootJar -Pvaadin.productionMode=true
```
