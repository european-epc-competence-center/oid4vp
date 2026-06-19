# DCQL query matching (wallet / credential store)

`DcqlQuery` helpers for mapping verifier DCQL to repository queries (`de.eecc.oid4vc.oid4vp.DcqlQuery`).

## `CredentialMatchCriteria`

Normalized snapshot: `credentialQueryId`, `format`, `typeValues`, `subjectIds`, `issuers`, `claimValueConstraints`, `requireCryptographicHolderBinding`.

Built via `CredentialQuery.toMatchCriteria()` or `Query.matchCriteria()`.

## `type_values` shape

`List<List<String>>`: outer list = OR of alternatives; inner list = AND of required `@type` strings (matches DCQL `meta.type_values`).

## `CredentialQuery` accessors

- `typeValues()` — from `meta.type_values`
- `subjectIds()` — `values` on claims with path `["credentialSubject", "id"]` (legacy `["id"]` normalized on construction)
- `issuers()` — `values` on claims with path `["issuer"]`
- `claimValueConstraints()` — claim id → required values (subject claims normalized under `credentialSubject`)
- `ClaimsQuery.subjectClaim()` / `normalizeClaimPath()` — bare paths prefixed with `credentialSubject`; `issuer` stays credential-level
- `valuesForClaimId()` / `pathForClaimId()` / `findClaim()`

## `PresentationRequestDefinition`

`templateClaimIds()`, `credentialMatchCriteria()`, `matchCriteriaForFormat()`, `requiredClaimValues()`.

Credential metadata from presentations: `templateCredentialTypes()`, `extractCredentialType()`, `extractCredentialIssuer()`, `extractCredentialSubjectId()` (via `PresentationParser`).

Optional identity constraints: list-based `requiredIssuers()` / `requiresIssuers()` and `requiredSubjectIds()` / `requiresSubjectIds()` on `PresentationRequestDefinition` (`requiresIssuer()` / `requiresSubjectId()` are single-value conveniences; DCQL `values` OR semantics); default `dcqlQuery()` merges constraints into `templateDcqlQuery()`.

Helper methods on DCQL records are `@JsonIgnore` for wire serialization.
