package de.eecc.oid4vc.oid4vp;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Digital Credentials Query Language (DCQL) models per OpenID4VP Section 6.
 *
 * <p>{@link CredentialQuery} exposes accessors ({@link #typeValues()}, {@link #subjectIds()},
 * {@link #claimValueConstraints()}) intended to map verifier queries to wallet credential stores.
 */
public final class DcqlQuery {

    /** DCQL claims path for credential subject {@code id}. */
    public static final String CREDENTIAL_SUBJECT = "credentialSubject";

    public static final List<Object> CLAIM_PATH_SUBJECT_ID = List.of(CREDENTIAL_SUBJECT, "id");

    /** DCQL claims path for credential {@code issuer}. */
    public static final List<Object> CLAIM_PATH_ISSUER = List.of("issuer");

    private DcqlQuery() {}

    /**
     * Builds a claims query for a path under {@link #CREDENTIAL_SUBJECT}. A leading
     * {@code credentialSubject} segment is added automatically when omitted.
     */
    public static ClaimsQuery subjectClaim(String id, List<Object> pathUnderSubject) {
        return new ClaimsQuery(id, pathUnderSubject);
    }

    public static ClaimsQuery subjectClaim(String id, List<Object> pathUnderSubject, List<String> values) {
        return new ClaimsQuery(id, pathUnderSubject, values);
    }

    static List<Object> normalizeClaimPath(List<Object> path) {
        if (path == null || path.isEmpty()) {
            return path;
        }
        List<String> segments = path.stream().map(Object::toString).toList();
        String first = segments.getFirst();
        if (isCredentialLevelPath(segments)) {
            return List.copyOf(path);
        }
        if (CREDENTIAL_SUBJECT.equals(first)) {
            return List.copyOf(path);
        }
        List<Object> normalized = new ArrayList<>();
        normalized.add(CREDENTIAL_SUBJECT);
        normalized.addAll(path);
        return List.copyOf(normalized);
    }

    private static boolean isCredentialLevelPath(List<String> segments) {
        if (segments.isEmpty()) {
            return false;
        }
        String first = segments.getFirst();
        return "issuer".equals(first) || "iss".equals(first);
    }

    /**
     * Normalized view of a single DCQL credential query for database or repository matching.
     */
    public record CredentialMatchCriteria(
            String credentialQueryId,
            String format,
            List<List<String>> typeValues,
            List<String> subjectIds,
            List<String> issuers,
            Map<String, List<String>> claimValueConstraints,
            boolean requireCryptographicHolderBinding
    ) {
        public CredentialMatchCriteria {
            typeValues = typeValues == null ? List.of() : List.copyOf(typeValues);
            subjectIds = subjectIds == null ? List.of() : List.copyOf(subjectIds);
            issuers = issuers == null ? List.of() : List.copyOf(issuers);
            claimValueConstraints = claimValueConstraints == null ? Map.of() : Map.copyOf(claimValueConstraints);
        }
    }

    @Builder
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record Query(
            @JsonProperty("credentials") List<CredentialQuery> credentials
    ) {
        @JsonIgnore
        public List<CredentialMatchCriteria> matchCriteria() {
            if (credentials == null || credentials.isEmpty()) {
                return List.of();
            }
            return credentials.stream().map(CredentialQuery::toMatchCriteria).toList();
        }

        @JsonIgnore
        public Optional<CredentialQuery> credentialById(String credentialQueryId) {
            if (credentials == null || credentialQueryId == null) {
                return Optional.empty();
            }
            return credentials.stream()
                    .filter(c -> credentialQueryId.equals(c.id()))
                    .findFirst();
        }

        @JsonIgnore
        public List<CredentialQuery> credentialsForFormat(String format) {
            if (credentials == null || format == null) {
                return List.of();
            }
            return credentials.stream().filter(c -> format.equals(c.format())).toList();
        }

        @JsonIgnore
        public Optional<CredentialQuery> firstCredentialForFormat(String format) {
            List<CredentialQuery> matches = credentialsForFormat(format);
            return matches.isEmpty() ? Optional.empty() : Optional.of(matches.getFirst());
        }
    }

    @Builder
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record CredentialQuery(
            @JsonProperty("id") String id,
            @JsonProperty("format") String format,
            @JsonProperty("meta") Map<String, Object> meta,
            @JsonProperty("claims") List<ClaimsQuery> claims,
            @JsonProperty("require_cryptographic_holder_binding")
            Boolean requireCryptographicHolderBinding
    ) {
        public CredentialQuery {
            if (requireCryptographicHolderBinding == null) {
                requireCryptographicHolderBinding = true;
            }
        }

        @JsonIgnore
        public CredentialMatchCriteria toMatchCriteria() {
            return new CredentialMatchCriteria(
                    id,
                    format,
                    typeValues(),
                    subjectIds(),
                    issuers(),
                    claimValueConstraints(),
                    requireCryptographicHolderBinding);
        }

        /**
         * Credential type sets from {@code meta.type_values}.
         */
        @JsonIgnore
        @SuppressWarnings("unchecked")
        public List<List<String>> typeValues() {
            if (meta == null) {
                return List.of();
            }
            Object raw = meta.get("type_values");
            if (!(raw instanceof List<?> outer)) {
                return List.of();
            }
            List<List<String>> result = new ArrayList<>();
            for (Object item : outer) {
                if (item instanceof List<?> inner) {
                    result.add(inner.stream().map(Object::toString).toList());
                }
            }
            return List.copyOf(result);
        }

        /**
         * Subject identifiers required by claims on path {@code ["id"]} (credential subject id).
         */
        @JsonIgnore
        public List<String> subjectIds() {
            if (claims == null) {
                return List.of();
            }
            return claims.stream()
                    .filter(ClaimsQuery::isSubjectIdPath)
                    .flatMap(c -> c.values().stream())
                    .distinct()
                    .toList();
        }

        /**
         * Required issuer values from claims on path {@code ["issuer"]}.
         */
        @JsonIgnore
        public List<String> issuers() {
            if (claims == null) {
                return List.of();
            }
            return claims.stream()
                    .filter(ClaimsQuery::isIssuerPath)
                    .flatMap(c -> c.values().stream())
                    .distinct()
                    .toList();
        }

        /**
         * Claim id (or last path segment) to required value constraints from the DCQL query.
         */
        @JsonIgnore
        public Map<String, List<String>> claimValueConstraints() {
            if (claims == null) {
                return Map.of();
            }
            Map<String, List<String>> constraints = new LinkedHashMap<>();
            for (ClaimsQuery claim : claims) {
                if (claim.values().isEmpty()) {
                    continue;
                }
                String key = claim.effectiveId();
                if (key != null) {
                    constraints.put(key, claim.values());
                }
            }
            return Map.copyOf(constraints);
        }

        @JsonIgnore
        public Optional<List<String>> valuesForClaimId(String claimId) {
            return findClaim(claimId).map(ClaimsQuery::values).filter(values -> !values.isEmpty());
        }

        @JsonIgnore
        public Optional<List<String>> pathForClaimId(String claimId) {
            return findClaim(claimId).map(ClaimsQuery::pathSegments);
        }

        @JsonIgnore
        public Optional<ClaimsQuery> findClaim(String claimId) {
            if (claims == null || claimId == null) {
                return Optional.empty();
            }
            return claims.stream().filter(c -> claimId.equals(c.effectiveId())).findFirst();
        }
    }

    @Builder
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record ClaimsQuery(
            @JsonProperty("id") String id,
            @JsonProperty("path") List<Object> path,
            @JsonProperty("values") List<String> values
    ) {
        public ClaimsQuery(String id, List<Object> path) {
            this(id, path, List.of());
        }

        public ClaimsQuery {
            values = values == null ? List.of() : List.copyOf(values);
            path = normalizeClaimPath(path);
        }

        @JsonIgnore
        public boolean isUnderCredentialSubject() {
            List<String> segments = pathSegments();
            return !segments.isEmpty() && CREDENTIAL_SUBJECT.equals(segments.getFirst());
        }

        @JsonIgnore
        public List<String> pathSegments() {
            if (path == null) {
                return List.of();
            }
            return path.stream().map(Object::toString).toList();
        }

        /**
         * Explicit {@code id} when set; otherwise the last segment of {@link #path}.
         */
        @JsonIgnore
        public String effectiveId() {
            if (id != null && !id.isBlank()) {
                return id;
            }
            List<String> segments = pathSegments();
            return segments.isEmpty() ? null : segments.getLast();
        }

        @JsonIgnore
        public boolean isSubjectIdPath() {
            List<String> segments = pathSegments();
            return segments.equals(List.of("id"))
                    || segments.equals(List.of(CREDENTIAL_SUBJECT, "id"));
        }

        @JsonIgnore
        public boolean isIssuerPath() {
            List<String> segments = pathSegments();
            return segments.size() == 1 && "issuer".equals(segments.getFirst());
        }
    }

    /**
     * Returns a copy of {@code query} with optional issuer and subject-id value constraints
     * merged into every credential query's claims.
     *
     * <p>Each non-empty list is written to the corresponding DCQL claim {@code values} array;
     * the wallet matches when the claim equals at least one listed value (OpenID4VP Section 6.3).
     */
    public static Query withIdentityConstraints(
            Query query, List<String> requiredIssuers, List<String> requiredSubjectIds) {
        if (query == null || query.credentials() == null || query.credentials().isEmpty()) {
            return query;
        }
        List<String> issuers = normalizeConstraintValues(requiredIssuers);
        List<String> subjectIds = normalizeConstraintValues(requiredSubjectIds);
        if (issuers.isEmpty() && subjectIds.isEmpty()) {
            return query;
        }
        List<CredentialQuery> credentials = query.credentials().stream()
                .map(credential -> withIdentityConstraints(credential, issuers, subjectIds))
                .toList();
        return new Query(credentials);
    }

    /**
     * Returns a copy of {@code credentialQuery} with optional issuer and subject-id value constraints
     * merged into its claims.
     */
    public static CredentialQuery withIdentityConstraints(
            CredentialQuery credentialQuery,
            List<String> requiredIssuers,
            List<String> requiredSubjectIds) {
        List<String> issuers = normalizeConstraintValues(requiredIssuers);
        List<String> subjectIds = normalizeConstraintValues(requiredSubjectIds);
        if (issuers.isEmpty() && subjectIds.isEmpty()) {
            return credentialQuery;
        }
        List<ClaimsQuery> claims = mergeIdentityConstraints(
                credentialQuery.claims(), issuers, subjectIds);
        return new CredentialQuery(
                credentialQuery.id(),
                credentialQuery.format(),
                credentialQuery.meta(),
                claims,
                credentialQuery.requireCryptographicHolderBinding());
    }

    static List<ClaimsQuery> mergeIdentityConstraints(
            List<ClaimsQuery> claims,
            List<String> requiredIssuers,
            List<String> requiredSubjectIds) {
        List<ClaimsQuery> merged = claims == null ? new ArrayList<>() : new ArrayList<>(claims);
        if (!requiredSubjectIds.isEmpty()) {
            upsertClaimValues(merged, "id", CLAIM_PATH_SUBJECT_ID, requiredSubjectIds);
        }
        if (!requiredIssuers.isEmpty()) {
            upsertClaimValues(merged, "issuer", CLAIM_PATH_ISSUER, requiredIssuers);
        }
        return List.copyOf(merged);
    }

    public static List<String> normalizeConstraintValues(List<String> values) {
        if (values == null || values.isEmpty()) {
            return List.of();
        }
        return values.stream()
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(value -> !value.isEmpty())
                .distinct()
                .toList();
    }

    private static void upsertClaimValues(
            List<ClaimsQuery> claims, String claimId, List<Object> path, List<String> values) {
        List<Object> normalizedPath = normalizeClaimPath(path);
        for (int i = 0; i < claims.size(); i++) {
            ClaimsQuery existing = claims.get(i);
            if (pathSegmentsEqual(existing.path(), normalizedPath)) {
                claims.set(i, new ClaimsQuery(
                        existing.id() != null ? existing.id() : claimId,
                        existing.path(),
                        values));
                return;
            }
        }
        claims.add(new ClaimsQuery(claimId, normalizedPath, values));
    }

    private static boolean pathSegmentsEqual(List<Object> left, List<Object> right) {
        return normalizeClaimPath(left).equals(normalizeClaimPath(right));
    }

    /**
     * Returns claim ids declared on the first credential query of a template definition.
     */
    public static List<String> claimIdsFromTemplate(Query query) {
        if (query == null || query.credentials() == null || query.credentials().isEmpty()) {
            return List.of();
        }
        return query.credentials().getFirst().claims() == null
                ? List.of()
                : query.credentials().getFirst().claims().stream()
                        .map(ClaimsQuery::effectiveId)
                        .filter(Objects::nonNull)
                        .distinct()
                        .toList();
    }
}
