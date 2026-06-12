# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

## [0.1.0] - 2026-06-12

### Added

- add release profile to pom
## [0.0.1] - 2026-06-12

### Added

- Initial Java library for OpenID4VP presentation request generation and processing
- `Oid4Vp` API: generate presentation requests, build wallet URLs, process direct_post responses, and poll verification status
- `PresentationRequestDefinition` interface with `PresentationRequest` / `PublicPresentationRequest` models and Lombok `@SuperBuilder` support for application-specific extensions
- DCQL query models (`DcqlQuery`) and OpenID4VP protocol constants
- Caffeine-backed `PresentationRequestStore` with optional `request_uri` transport
- `VerifierClient` for credential verification API integration (compatible with the [EECC VC Verifier](https://github.com/european-epc-competence-center/vc-verifier))
- `PresentationParser` for extracting claims from `vp_token` JSON responses
- GS1 license presentation template (`Gs1LicenseRequest`) for Company Prefix and Prefix License credentials
- `DirectPostHandler` functional interface for post-verification application logic
- Automated release script with changelog detection (`scripts/release.js`)
- CI workflow (Java 25, Maven verify) and Maven Central release workflow on version tags

