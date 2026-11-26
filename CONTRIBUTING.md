# Contributing to PharmaLink

Contributions, documentation improvements, and bug fixes are welcome.

## Development Workflow

1. Fork the repository and create a short-lived branch from `main` using a descriptive prefix (`feat/...`, `fix/...`, `docs/...`, `ci/...`).
2. Follow [Conventional Commits](https://www.conventionalcommits.org/) for commit messages (`feat:`, `fix:`, `docs:`, `chore:`, `ci:`).
3. Keep documentation synchronized with any script, endpoint, or contract changes in the same pull request.

## Local Build Checks

Before opening a pull request, verify affected modules from the repository root:

```bash
# Backend (JDK 21)
./gradlew compileJava compileTestJava test --no-daemon

# Frontend (JDK 21)
./gradlew -p frontend compileJava --no-daemon

# Chaincode (JDK 11)
./gradlew -p chaincode/pharmalink_chaincode_main compileJava checkstyleMain --no-daemon

# Shell scripts
for f in scripts/*.sh scripts/*/*.sh common/organizations/fabric-ca/registerEnroll.sh; do bash -n "$f"; done
```

## Pull Request Guidelines

- Do not commit personal paths (`/Users/...`, `/home/...`), private hostnames, or generated `fabric-samples/` crypto outputs.
- Preserve existing MIT licensing headers and test-network safety disclaimers.
- Use the [Pull Request Template](./.github/PULL_REQUEST_TEMPLATE.md) when submitting changes.
