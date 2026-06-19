# oid4vp Project Overview

## Project Summary

Java library for generating and processing [OpenID4VP](https://openid.net/specs/openid-4-verifiable-presentations-1_0.html) presentation requests.

**Current Version**: 0.4.1-SNAPSHOT (see `oid4vp-java/pom.xml` and module parent references)  
**License**: Apache License 2.0  
**Technology Stack**: Java 25, Maven, Jackson, Caffeine, SLF4J; optional Spring Boot 3.4  
**Maven coordinates**: `de.eecc.oid4vc:oid4vp` (core), `de.eecc.oid4vc:oid4vp-spring-boot-starter`

## Project Structure

```
oid4vp/
├── oid4vp-java/                      # Multi-module Maven parent
│   ├── pom.xml                       # oid4vp-parent — version source for releases
│   ├── oid4vp-core/                  # Framework-neutral library (artifact: oid4vp)
│   ├── oid4vp-spring/                # Spring Boot auto-configuration
│   └── oid4vp-spring-boot-starter/   # Starter dependency aggregator
├── scripts/release.js
├── CHANGELOG.md
└── README.md
```

See [module-layout.md](module-layout.md) for embedding patterns, pluggable dependencies, and Spring setup.  
See [dcql-query-matching.md](dcql-query-matching.md) for DCQL credential-store match helpers.

## Core Features

- **Presentation request generation** via `Oid4Vp.generatePresentationRequest()`
- **Wallet URL building** with inline or `request_uri` transport
- **Direct post handling** with optional `response_code` (`DirectPostResult`) — OAuth2 login completion documented in `README.md`
- **Pluggable** repository and verifier; `Oid4Vp.builder()` for tests and host wiring
- **DCQL query models** with credential-store match helpers (`CredentialMatchCriteria`, `typeValues()`, `subjectIds()`, …) — see [dcql-query-matching.md](dcql-query-matching.md); GS1 template, `PresentationParser`, `PresentationClaims` extraction via `PresentationRequestDefinition`, sealed `Oid4VpError`

## Development & Release

```bash
cd oid4vp-java
mvn test
mvn package
```

CI (`.github/workflows/ci.yml`): on push to `main` and on pull requests — Java 25, `mvn test` in `oid4vp-java/`.

Release: `npm run release minor` (from repo root). Version lives in parent `oid4vp-parent` POM and must match the `<parent><version>` in `oid4vp-core`, `oid4vp-spring`, and `oid4vp-spring-boot-starter`. `minor`/`major` bumps use the latest git tag as the base; `patch` uses the SNAPSHOT version in the POM. See `scripts/release.js`.

## Important Files

- `oid4vp-java/oid4vp-core/src/main/java/de/eecc/oid4vc/oid4vp/api/Oid4Vp.java` — main entry point
- `CHANGELOG.md` — version history
