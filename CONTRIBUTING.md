# Contributing to PharmaLink

> **Maintenance Note:** PharmaLink was built as a hands-on demo alongside a Bachelor's thesis to work through the concepts in practice; it is completed and no longer actively developed or maintained. Feel free to fork the repository for your own experiments; upstream pull requests are reviewed only on a best-effort basis.

## Development Workflow

1. Fork the repository and create a short-lived branch from `main` using a descriptive prefix (`feat/...`, `fix/...`, `docs/...`, `ci/...`).
2. Follow [Conventional Commits](https://www.conventionalcommits.org/) for commit messages (`feat:`, `fix:`, `docs:`, `chore:`, `ci:`).
3. Keep documentation synchronized with any script, endpoint, or contract changes in the same pull request.

## Local Build Checks

Before opening a pull request, run the full check suite inside the Nix dev shell (automatically provides JDK 21, JDK 11, `just`, `python3`, and `ruby`):

```bash
nix develop -c just doctor
nix develop -c just check
```

Without Nix (requires local JDK 21 + JDK 11):

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

- Do not commit host-specific home directory paths, private hostnames, or generated `fabric-samples/` crypto outputs.
- Preserve existing MIT licensing headers and test-network safety disclaimers.
- Use the [Pull Request Template](./.github/PULL_REQUEST_TEMPLATE.md) when submitting changes.
