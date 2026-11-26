# Security Policy

## Scope & Test-Network Disclaimer

PharmaLink is a **proof-of-concept and evaluation test platform** built on Hyperledger Fabric's local `test-network` topology.

> [!CAUTION]
> **Do not deploy repository defaults to production.** The following development placeholders are included solely to let contributors boot the local test stack cleanly:
> - Default application login (`admin` / `admin123`) in `src/main/resources/application.yaml` and `docker/pharmalink/application.yaml`.
> - Default MongoDB credentials (`user` / `user`) in `docker/docker-compose-pharmalink.yaml`.
> - Default Base64 development `JWT_SECRET` in Spring configuration files.
> - Test-network CA bootstrap credentials (`admin` / `adminpw`) and static peer/user crypto snapshots in [`evaluation/msp/organizations.zip`](./evaluation/msp/organizations.zip).

## Hardening Requirements for Non-Local Use

If adapting this architecture beyond local evaluation:

1. Override `JWT_SECRET`, `SYSTEM_PASSWORD`, `MONGO_ROOT_USER`, `MONGO_ROOT_PASSWORD`, and `MONGODB_URI` via an external secrets store.
2. Replace `CorsFilter` wildcard origins in [`SecurityConfig.java`](./src/main/java/de/jklein/pharmalink/config/SecurityConfig.java) with explicit trusted origin patterns.
3. Provision independent organization CAs with mutual TLS (`mTLS`) and strict multi-org Fabric endorsement policies instead of single-host test certificates.
4. Place Spring Boot (`:8080`) and Vaadin (`:8083`) behind a TLS-terminating reverse proxy and restrict direct access to MongoDB (`:27017`), IPFS RPC (`:5001`), and Fabric ports.

## Reporting a Vulnerability

If you discover a security issue in the codebase or workflows:

- Open a private security advisory on GitHub (**Security → Advisories → Report a vulnerability**) or contact the repository owner (`@NiklasJavier`) before public disclosure.
- Include reproduction steps, affected component (`backend`, `frontend`, `chaincode`, `scripts`), and potential impact.
