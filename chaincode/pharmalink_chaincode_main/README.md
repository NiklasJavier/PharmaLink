# `pharmalink_chaincode_main`

Java 11 Hyperledger Fabric Smart Contract (`PharmaSupplyChainContract`) packaged as an executable Shadow JAR for Fabric `v2.5`.

## Build & Quality Commands

Run from the repository root (`PharmaLink/`) with **JDK 11** installed:

```bash
# Compile Java sources and run Checkstyle
./gradlew -p chaincode/pharmalink_chaincode_main compileJava checkstyleMain

# Build distribution for Fabric deployment (skips JaCoCo 90% gate while unit tests are pending)
./gradlew -p chaincode/pharmalink_chaincode_main installDist -x jacocoTestCoverageVerification

# Package as a Hyperledger Fabric external/lifecycle chaincode tarball
peer lifecycle chaincode package pharmalink_chaincode_main.tar.gz \
  --path ./chaincode/pharmalink_chaincode_main \
  --lang java \
  --label pharmalink_chaincode_main_1.0
```

> **Note:** `chaincode/pharmalink_chaincode_main/build.gradle` configures `jacocoTestCoverageVerification` with `minimum = 0.9` on `check` and `installDist`. Until JUnit contract tests are added under `src/test/java`, pass `-x jacocoTestCoverageVerification` when building distributions or Docker images.

See [`../README.md`](../README.md) for the complete catalog of 30 transactions, 7 CouchDB indexes, and 14 emitted events.
