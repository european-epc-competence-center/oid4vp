# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

## [0.5.1] - 2026-06-18

### Changed

- `VerificationFailed` now surfaces vc-verifier error messages from `error.name` (and related fields) instead of a generic failure text
- Documented OAuth 2.0 login completion via `response_code` in `README.md`
## [0.5.0] - 2026-06-15

### Added

- `PresentationRequestDefinition.extractPresentationClaims(JsonNode)` for template-specific claim extraction from verified `vp_token` responses
- `Oid4Vp.extractPresentationClaims(...)` helpers to parse stored tokens and validate primary claim values
- `EmptyPresentationClaims` error (HTTP 401) when extracted `PresentationClaims.values()` is empty
## [0.4.3] - 2026-06-15

- merge ttl into one property
## [0.4.2] - 2026-06-15

### Changed

- **Breaking:** Merged `authorizationRequestTtl` into `requestTtl` on `Oid4VpOptions` and `Oid4VpProperties`; one TTL now controls both request expiry (`expiresAt`) and the default Caffeine repository eviction
## [0.4.1] - 2026-06-15

### Fixed

- Release script `minor`/`major` bumps now derive from the last git tag instead of the SNAPSHOT version in `pom.xml`, preventing skipped versions when the POM was manually advanced ahead of the last release
- Release script now updates the parent POM and all module POM parent version references together
## [0.2.0] - 2026-06-15

### Added

- Multi-module Maven layout: `oid4vp-core` (`de.eecc.oid4vc:oid4vp`), `oid4vp-spring`, `oid4vp-spring-boot-starter`
- `PresentationRequestRepository` and `PresentationVerifier` interfaces with Caffeine/HTTP default implementations
- `Oid4Vp.builder()` for injectable repository, verifier, `ObjectMapper`, and `SecureRandom`
- `DirectPostOutcome`, `DirectPostResult`, and optional `response_code` issuance via handler return value
- `presentationComplete` flag on `PresentationRequest` for non-login direct_post flows
- `GenerateRequestOptions.beforeSave` hook and `builderSupplier` documentation for pre-persistence customization
- `PollStatusResolver` and `completed` field on `VpTokenResponse.PollResponse` (`PollResponse.asCompleted()`)
- Sealed `Oid4VpError` model with typed errors (`UnknownState`, `VerificationFailed`, etc.)
- Spring Boot auto-configuration (`oid4vp.*` properties, `@Bean Oid4Vp`, optional `Oid4VpExceptionHandler`)

### Changed

- **Breaking:** `DirectPostHandler.onVerified` now returns `DirectPostResult` instead of `DirectPostResponse`
- **Breaking:** `Oid4VpException` is constructed with `Oid4VpError`; use `error().suggestedHttpStatus()` for HTTP mapping
- `Oid4Vp.create(Oid4VpOptions)` delegates to `Oid4Vp.builder()`
- Removed duplicate `de.eecc.oid4vp.*` package tree; canonical package is `de.eecc.oid4vc.oid4vp.*`
- Renamed `PresentationRequestStore` → `CaffeinePresentationRequestRepository`, `VerifierClient` → `HttpPresentationVerifier`
- Pinned Jackson to `${jackson.version}` (2.18.3) to avoid `RELEASE` version skew at test runtime
## [0.1.1] - 2026-06-12

### Fixed

- Fix Maven Central publishing: release profile now generates and attaches the Javadoc JAR required by Sonatype Central
- Pin Lombok to 1.18.38 for JDK 25 compatibility in the delombok step used before Javadoc generation
- Configure `lombok-maven-plugin` to delombok from `src/main/java` instead of the default `src/main/lombok` path, which caused delombok to be skipped and no Javadoc artifact to be published
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

