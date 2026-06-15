# Module layout and embedding

## Maven modules (`oid4vp-java/`)

| Module | Artifact | Role |
|--------|----------|------|
| Parent | `oid4vp-parent` | BOM, build plugins, version |
| Core | `oid4vp` | Framework-neutral protocol API |
| Spring | `oid4vp-spring` | `@ConfigurationProperties`, auto-config, optional web exception handler |
| Starter | `oid4vp-spring-boot-starter` | Pulls in `oid4vp-spring` + `AutoConfiguration.imports` |

## Core entry points

- `Oid4Vp.create(options)` or `Oid4Vp.builder()` — main facade
- `PresentationRequestRepository` / `CaffeinePresentationRequestRepository` — pluggable store
- `PresentationVerifier` / `HttpPresentationVerifier` — pluggable verifier HTTP client
- `DirectPostHandler` → `DirectPostResult` — post-verification outcome (`ISSUE_RESPONSE_CODE`, `COMPLETE`, `CUSTOM`)
- `GenerateRequestOptions.builderSupplier` + `beforeSave` — set app fields before `save()`
- `PollStatusResolver` — override poll UX; default handles `response_code`, `completed`, verification errors
- OAuth2 login: `issueResponseCode` → poll or redirect → redeem `state`+`response_code` at token endpoint → `invalidateResponseCode`
- `Oid4VpError` sealed hierarchy — map errors without parsing HTTP status from messages
- `PresentationRequestDefinition.extractPresentationClaims` + `Oid4Vp.extractPresentationClaims` — template-driven claim extraction from stored `vp_token`

## Spring Boot (optional)

```yaml
oid4vp:
  verifier-url: http://vc-verifier:3000/api/verifier
  response-uri: https://example.com/api/auth/oid4vp/response
```

Dependency: `de.eecc.oid4vc:oid4vp-spring-boot-starter`. Registers `@Bean Oid4Vp` (not `@Service`). No REST controllers.

## Package

Canonical package: `de.eecc.oid4vc.oid4vp.*` only.
