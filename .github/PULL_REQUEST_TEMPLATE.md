## Summary
<!-- Describe the change and link any related issue. -->

## Type of Change
- [ ] Bug fix (`fix`)
- [ ] Feature (`feat`)
- [ ] Documentation (`docs`)
- [ ] Scripts / Infra / CI (`chore`, `ci`)

## Verification
- [ ] Backend: `./gradlew compileJava compileTestJava test --no-daemon`
- [ ] Frontend: `./gradlew -p frontend compileJava --no-daemon`
- [ ] Chaincode (JDK 11): `./gradlew -p chaincode/pharmalink_chaincode_main compileJava checkstyleMain --no-daemon`
- [ ] Scripts: `bash -n scripts/*.sh scripts/*/*.sh`
- [ ] No hardcoded local home paths, private hostnames, or real secrets committed
