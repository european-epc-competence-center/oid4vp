# oid4vp Project Overview

## Project Summary

Java library for generating and processing [OpenID4VP](https://openid.net/specs/openid-4-verifiable-presentations-1_0.html) presentation requests.

**Current Version**: 0.2.0-SNAPSHOT (see `oid4vp-java/pom.xml`)  
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
├── docs/API_INTEGRATION_ROADMAP.md   # Embedding / discovery-service integration plan
├── scripts/release.js
├── CHANGELOG.md
└── README.md
```

See [module-layout.md](module-layout.md) for embedding patterns, pluggable dependencies, and Spring setup.

## Core Features

- **Presentation request generation** via `Oid4Vp.generatePresentationRequest()`
- **Wallet URL building** with inline or `request_uri` transport
- **Direct post handling** with optional `response_code` (`DirectPostResult`)
- **Pluggable** repository and verifier; `Oid4Vp.builder()` for tests and host wiring
- **DCQL query models**, GS1 template, `PresentationParser`, sealed `Oid4VpError`

## Development & Release

```bash
cd oid4vp-java
mvn test
mvn package
```

Release: `npm run release minor` (from repo root). Version lives in parent `oid4vp-parent` POM.

## Important Files

- `oid4vp-java/oid4vp-core/src/main/java/de/eecc/oid4vc/oid4vp/api/Oid4Vp.java` — main entry point
- `docs/API_INTEGRATION_ROADMAP.md` — full refactor rationale and discovery-service notes
- `CHANGELOG.md` — version history
