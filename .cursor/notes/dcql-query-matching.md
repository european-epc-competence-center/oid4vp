# DCQL query matching (wallet / credential store)

`DcqlQuery` helpers for mapping verifier DCQL to repository queries (`de.eecc.oid4vc.oid4vp.DcqlQuery`).

## `CredentialMatchCriteria`

Normalized snapshot: `credentialQueryId`, `format`, `typeValues`, `subjectIds`, `issuers`, `credentialIds`, `claimValueConstraints`, `requireCryptographicHolderBinding`.

Built via `CredentialQuery.toMatchCriteria()` or `Query.matchCriteria()`.

## `type_values` shape

`List<List<String>>`: outer list = OR of alternatives; inner list = AND of required `@type` strings (matches DCQL `meta.type_values`).

## `CredentialQuery` accessors

- `typeValues()` — from `meta.type_values`
- `subjectIds()` — `values` on claims with path `["credentialSubject", "id"]`
- `credentialIds()` — `values` on claims with path `["id"]` (the W3C credential id, claim id `credentialId`)
- `issuers()` — `values` on claims with path `["issuer"]`
- `claimValueConstraints()` — claim id → required values (subject claims normalized under `credentialSubject`)
- `normalizeClaimPath()` prefixes bare paths with `credentialSubject`; `issuer` and a single-segment `id` stay credential-level (`isCredentialIdPath`). `subjectClaim()` always prefixes, so a bare `id` becomes `credentialSubject.id`
- `valuesForClaimId()` / `pathForClaimId()` / `findClaim()`

## `PresentationRequestDefinition`

`templateClaimIds()`, `credentialMatchCriteria()`, `matchCriteriaForFormat()`, `requiredClaimValues()`.

Credential metadata from presentations: `templateCredentialTypes()`, `extractCredentialType()`, `extractCredentialIssuer()`, `extractCredentialSubjectId()` (via `PresentationParser`).

Optional identity constraints: `requiredIssuers()` / `requiresIssuers()`, `requiredSubjectIds()` / `requiresSubjectIds()`, and `requiredCredentialIds()` / `requiresCredentialIds()` on `PresentationRequestDefinition` (single-value conveniences `requiresIssuer()`, `requiresSubjectId()`, `requiresCredentialId()`; DCQL `values` OR semantics); default `dcqlQuery()` merges constraints into `templateDcqlQuery()`.

Helper methods on DCQL records are `@JsonIgnore` for wire serialization.
