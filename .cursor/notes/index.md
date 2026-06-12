# oid4vp Project Overview

## Project Summary

Java library for generating and processing [OpenID4VP](https://openid.net/specs/openid-4-verifiable-presentations-1_0.html) presentation requests.

**Current Version**: 0.1.0-SNAPSHOT (see `oid4vp-java/pom.xml`)  
**License**: Apache License 2.0  
**Technology Stack**: Java 25, Maven, Jackson, Caffeine, SLF4J  
**Maven coordinates**: `de.eecc.oid4vc:oid4vp`

## Project Structure

```
oid4vp/
├── oid4vp-java/                 # Java library (Maven module)
│   ├── pom.xml                  # Version source for releases
│   └── src/main/java/de/eecc/oid4vc/oid4vp/
│       ├── api/                 # Public API: Oid4Vp, *Options, DirectPostHandler
│       ├── request/             # PresentationRequest, PresentationRequestDefinition
│       │   └── template/gs1/    # Built-in GS1 license presentation template
│       ├── store/               # PresentationRequestStore (Caffeine-backed)
│       ├── verifier/            # VerifierClient (EECC VC Verifier compatible)
│       ├── vp/                  # PresentationParser
│       └── exception/           # Oid4VpException
├── scripts/release.js           # Release automation (bumps pom, updates CHANGELOG, tags)
├── CHANGELOG.md                 # Keep a Changelog format (see .cursor/rules/changelog-conventions.mdc)
├── .github/workflows/
│   ├── ci.yml                   # Build & test on main
│   └── release.yml              # Maven Central publish + GitHub release on tag
└── README.md
```

## Core Features

- **Presentation request generation** via `Oid4Vp.generatePresentationRequest()`
- **Wallet URL building** with inline or `request_uri` transport
- **Direct post handling** with verifier integration and optional redirect flow
- **DCQL query models** and protocol constants
- **GS1 template** (`Gs1LicenseRequest`) for Company Prefix / Prefix License credentials
- **Claim extraction** via `PresentationParser` and template helpers

## Development & Release

```bash
cd oid4vp-java
mvn test
mvn package
```

Release workflow (from repo root):

```bash
npm run release patch   # 0.1.0-SNAPSHOT -> 0.1.0 -> 0.1.1-SNAPSHOT
npm run release minor   # 0.1.0-SNAPSHOT -> 0.2.0 -> 0.2.1-SNAPSHOT
npm run release major   # 0.1.0-SNAPSHOT -> 1.0.0 -> 1.0.1-SNAPSHOT
```

Maven versioning follows the company-wallet pattern: **patch** strips `-SNAPSHOT` only, **minor/major** bump from the base version. After tagging, the script commits the next patch `-SNAPSHOT` version.

See `.cursor/rules/changelog-conventions.mdc` for changelog rules. Add entries under `## [Unreleased]` before running the release script.

## Important Files

- `README.md` — usage examples and project structure
- `oid4vp-java/src/main/java/de/eecc/oid4vc/oid4vp/api/Oid4Vp.java` — main library entry point
- `oid4vp-java/pom.xml` — Maven version and build configuration
- `CHANGELOG.md` — version history
