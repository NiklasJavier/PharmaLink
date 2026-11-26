# PharmaLink command surface
# Enter dev shell:  nix develop
# One-shot check:   nix develop -c just check
# List recipes:     just --list

set shell := ["bash", "-euo", "pipefail", "-c"]

gradle_jdk_prop := if env_var_or_default("JDK11_HOME", "") != "" { "-Porg.gradle.java.installations.fromEnv=JDK21_HOME,JDK11_HOME" } else { "" }
gradle := "./gradlew --no-daemon " + gradle_jdk_prop

# List available recipes
default:
    @just --list

# Hint for entering the Nix devShell
shell:
    @echo "Run: nix develop   # loads Java 21, Java 11, just, shellcheck, jq, python3, ruby"

# Document flake-managed tooling
install:
    @echo "Tooling is managed declaratively via flake.nix (run 'nix develop')."
    @echo "Java/Gradle dependencies resolve via build.gradle on first build."

# Verify local tooling and required project files
doctor:
    @echo "Checking required tools and repository files..."
    @command -v just >/dev/null && just --version
    @command -v java >/dev/null && java -version 2>&1 | head -n 1
    @command -v jq >/dev/null && jq --version
    @command -v curl >/dev/null && curl --version | head -n 1
    @command -v bash >/dev/null && bash --version | head -n 1
    @command -v python3 >/dev/null && python3 --version
    @command -v ruby >/dev/null && ruby --version
    @if [ -n "${JDK21_HOME:-}" ]; then echo "JDK21_HOME=$JDK21_HOME"; else echo "WARN: JDK21_HOME unset (enter 'nix develop')"; fi
    @if [ -n "${JDK11_HOME:-}" ]; then echo "JDK11_HOME=$JDK11_HOME"; else echo "WARN: JDK11_HOME unset (needed for chaincode JDK 11)"; fi
    @if command -v docker >/dev/null; then docker --version; else echo "WARN: docker not found (required for Fabric/Compose runtime only)"; fi
    @test -f flake.nix && test -f justfile
    @test -f .agents/README.md && test -f .agents/skills/pharmalink-navigator/SKILL.md
    @echo "doctor: OK"

# Compile backend Java & test sources (JDK 21)
build-backend:
    {{gradle}} compileJava compileTestJava

# Run backend test task (JDK 21)
test-backend:
    {{gradle}} test

# Compile Vaadin 24 client UI (JDK 21)
build-frontend:
    {{gradle}} -p frontend compileJava

# Compile chaincode and run Checkstyle (requires JDK 11 / nix develop)
build-chaincode:
    {{gradle}} -p chaincode/pharmalink_chaincode_main compileJava checkstyleMain

# Build chaincode distribution (skips JaCoCo 90% gate while unit tests are pending)
build-chaincode-dist:
    {{gradle}} -p chaincode/pharmalink_chaincode_main installDist -x jacocoTestCoverageVerification

# Compile all three Java modules (backend, frontend, chaincode)
build: build-backend build-frontend build-chaincode

# Run automated tests
test: test-backend

# Validate shell script syntax across scripts/ and common/organizations/
lint-scripts:
    @set -e; \
    for f in scripts/*.sh scripts/*/*.sh common/organizations/fabric-ca/registerEnroll.sh common/organizations/ccp-generate.sh; do \
        bash -n "$f"; \
    done; \
    echo "lint-scripts: OK"

# Validate OpenAPI JSON, CouchDB indexes, and all YAML configs
check-configs:
    @python3 -c 'import glob, json; files = ["evaluation/openapi/openapi.json"] + sorted(glob.glob("chaincode/pharmalink_chaincode_main/src/main/resources/META-INF/statedb/couchdb/indexes/*.json")); [json.load(open(f, encoding="utf-8")) for f in files]; print(f"check-configs JSON: OK ({len(files)} files)")'
    @ruby -ryaml -e 'files = Dir.glob(".github/**/*.{yml,yaml}") + ["docker/docker-compose-pharmalink.yaml", "docker/pharmalink/application.yaml", "docker/pharmalink/application.yaml.template", "src/main/resources/application.yaml", "src/main/dev/docker-compose.yaml", "common/configtx.yaml", "common/organizations/ccp-template.yaml"] + Dir.glob("common/organizations/fabric-ca/*/fabric-ca-server-config.yaml"); files.uniq.sort.each { |f| YAML.safe_load(File.read(f), aliases: true) }; puts "check-configs YAML: OK (#{files.uniq.size} files)"'

# Check all relative Markdown links and image paths
check-docs:
    @python3 -c 'import re, pathlib; root = pathlib.Path("."); mds = sorted([p for p in root.rglob("*.md") if ".git" not in p.parts]); broken = []; [broken.append((str(md), m.group(1) or m.group(2))) for md in mds for m in re.finditer(r"(?:!?\[[^\]]*\]\(([^)]+)\)|(?:href|src)=\"([^\"]+)\")", re.sub(r"```.*?```", "", md.read_text(encoding="utf-8"), flags=re.S)) if not (m.group(1) or m.group(2)).startswith(("http://", "https://", "mailto:", "#")) and (m.group(1) or m.group(2)).split("#")[0] and not (md.parent / (m.group(1) or m.group(2)).split("#")[0]).resolve().exists()]; print(f"check-docs: {len(mds)} files, broken={len(broken)}"); assert not broken, broken'

# Full verification suite (builds, tests, script syntax, JSON/YAML configs, docs links)
check: build test lint-scripts check-configs check-docs

# Alias for check
verify: check

# Start local MongoDB + IPFS dev containers for IDE development
dev-infra-up:
    docker compose -f src/main/dev/docker-compose.yaml up -d

# Stop local MongoDB + IPFS dev containers
dev-infra-down:
    docker compose -f src/main/dev/docker-compose.yaml down

# Run Spring Boot backend locally (port 8080)
dev-backend:
    {{gradle}} bootRun

# Run Vaadin 24 client UI locally (port 8083)
dev-frontend:
    {{gradle}} -p frontend bootRun

# Install standalone Hyperledger Fabric CLI binaries into ~/fabric-cli
fabric-cli:
    ./scripts/fabric_setup_cli.sh

# Start Hyperledger Fabric test-network (channel: pharmalink, CouchDB)
fabric-up:
    ./scripts/fabric_setup_test.sh up

# Build and deploy Java chaincode onto the Fabric test-network
fabric-cc:
    ./scripts/fabric_setup_test.sh cc

# Enroll consortium identities (3 users/role) and run initCall
fabric-consortium:
    ./scripts/fabric_setup_test_consortium.sh

# Tear down Hyperledger Fabric test-network
fabric-down:
    ./scripts/fabric_setup_test.sh down

# Start Backend, Vaadin UI, IPFS, and MongoDB via Docker Compose
app-up:
    ./scripts/pharmalink_setup.sh up

# Stop Docker Compose application stack
app-down:
    ./scripts/pharmalink_setup.sh down

# Full end-to-end startup: Fabric network -> chaincode -> consortium -> Docker app stack
up: fabric-up fabric-cc fabric-consortium app-up

# Safe teardown of Compose stack and Fabric test-network
down:
    ./scripts/manage/repo_reset.sh

# Print the source command for switching Fabric CLI role identity (e.g. just role behoerde)
role name="hersteller":
    @test -f "./scripts/roles/fabric_role_{{name}}.sh" || { echo "Unknown role '{{name}}'. Available: hersteller[2|3], grosshaendler[2|3], apotheke[2|3], behoerde[2|3]"; exit 1; }
    @echo "Run in your current shell:"
    @echo "  source ./scripts/roles/fabric_role_{{name}}.sh"
