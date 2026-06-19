package de.eecc.oid4vc.oid4vp.request;

import com.fasterxml.jackson.databind.JsonNode;
import de.eecc.oid4vc.oid4vp.ClientMetadata;
import de.eecc.oid4vc.oid4vp.DcqlQuery;
import de.eecc.oid4vc.oid4vp.PresentationClaims;
import de.eecc.oid4vc.oid4vp.vp.PresentationParser;

import java.util.List;
import java.util.Optional;

/**
 * Credential-specific parts of an OID4VP authorization request.
 * Transport fields (nonce, state, client_id, response_uri) are supplied by {@link de.eecc.oid4vc.oid4vp.api.Oid4Vp}.
 */
public interface PresentationRequestDefinition {

    /**
     * Template DCQL query without optional identity constraints.
     */
    DcqlQuery.Query templateDcqlQuery();

    /**
     * DCQL query for this presentation request, including identity constraints from
     * {@link #requiresIssuers(List)} or {@link #requiresSubjectIds(List)}.
     */
    default DcqlQuery.Query dcqlQuery() {
        return DcqlQuery.withIdentityConstraints(templateDcqlQuery(), requiredIssuers(), requiredSubjectIds());
    }

    /**
     * Root template when this definition was produced by chaining identity constraints.
     */
    default PresentationRequestDefinition root() {
        return this;
    }

    /**
     * Required credential issuer values merged into {@link #dcqlQuery()} (OR semantics per DCQL {@code values}).
     */
    default List<String> requiredIssuers() {
        return List.of();
    }

    /**
     * Required credential subject ids merged into {@link #dcqlQuery()} (OR semantics per DCQL {@code values}).
     */
    default List<String> requiredSubjectIds() {
        return List.of();
    }

    /**
     * Returns a definition that requires at least one of the given credential issuers in the DCQL query.
     */
    default PresentationRequestDefinition requiresIssuers(List<String> requiredIssuers) {
        return withIdentityConstraints(root(), requiredIssuers, requiredSubjectIds());
    }

    /**
     * Returns a definition that requires at least one of the given credential subject ids in the DCQL query.
     */
    default PresentationRequestDefinition requiresSubjectIds(List<String> requiredSubjectIds) {
        return withIdentityConstraints(root(), requiredIssuers(), requiredSubjectIds);
    }

    /** Convenience for {@link #requiresIssuers(List)} with a single issuer. */
    default PresentationRequestDefinition requiresIssuer(String requiredIssuer) {
        return requiresIssuers(List.of(requiredIssuer));
    }

    /** Convenience for {@link #requiresSubjectIds(List)} with a single subject id. */
    default PresentationRequestDefinition requiresSubjectId(String requiredSubjectId) {
        return requiresSubjectIds(List.of(requiredSubjectId));
    }

    private static PresentationRequestDefinition withIdentityConstraints(
            PresentationRequestDefinition root,
            List<String> requiredIssuers,
            List<String> requiredSubjectIds) {
        List<String> issuers = DcqlQuery.normalizeConstraintValues(requiredIssuers);
        List<String> subjectIds = DcqlQuery.normalizeConstraintValues(requiredSubjectIds);
        if (issuers.isEmpty() && subjectIds.isEmpty()) {
            return root;
        }
        return new PresentationRequestDefinition() {
            @Override
            public PresentationRequestDefinition root() {
                return root;
            }

            @Override
            public DcqlQuery.Query templateDcqlQuery() {
                return root.templateDcqlQuery();
            }

            @Override
            public List<String> requiredIssuers() {
                return issuers;
            }

            @Override
            public List<String> requiredSubjectIds() {
                return subjectIds;
            }

            @Override
            public ClientMetadata clientMetadata() {
                return root.clientMetadata();
            }

            @Override
            public PresentationClaims extractPresentationClaims(JsonNode vpTokenNode) {
                return root.extractPresentationClaims(vpTokenNode);
            }
        };
    }

    default ClientMetadata clientMetadata() {
        return ClientMetadata.presentationDefault();
    }

    /**
     * Claim ids from this template's DCQL query (first credential query), for wallet-side matching.
     */
    default List<String> templateClaimIds() {
        return DcqlQuery.claimIdsFromTemplate(dcqlQuery());
    }

    /**
     * Normalized match criteria for all credential queries in this template.
     */
    default List<DcqlQuery.CredentialMatchCriteria> credentialMatchCriteria() {
        return dcqlQuery().matchCriteria();
    }

    /**
     * Required claim values for {@code claimId} on the first credential query of this template.
     */
    default Optional<List<String>> requiredClaimValues(String claimId) {
        DcqlQuery.Query query = dcqlQuery();
        if (query.credentials() == null || query.credentials().isEmpty()) {
            return Optional.empty();
        }
        return query.credentials().getFirst().valuesForClaimId(claimId);
    }

    /**
     * Match criteria for a specific presentation format declared by this template.
     */
    default Optional<DcqlQuery.CredentialMatchCriteria> matchCriteriaForFormat(String format) {
        return dcqlQuery().firstCredentialForFormat(format).map(DcqlQuery.CredentialQuery::toMatchCriteria);
    }

    /**
     * Specific credential types from this template's DCQL {@code meta.type_values} (excluding
     * {@code VerifiableCredential}).
     */
    default List<String> templateCredentialTypes() {
        return credentialMatchCriteria().stream()
                .flatMap(criteria -> criteria.typeValues().stream())
                .flatMap(List::stream)
                .filter(type -> !"VerifiableCredential".equals(type))
                .distinct()
                .toList();
    }

    /**
     * Returns the credential type from the first matching verifiable credential in a presentation,
     * filtered to {@link #templateCredentialTypes()} when declared.
     */
    default String extractCredentialType(JsonNode presentationNode) {
        return PresentationParser.extractCredentialType(presentationNode, templateCredentialTypes());
    }

    /** Returns the issuer from the first verifiable credential in a presentation. */
    default String extractCredentialIssuer(JsonNode presentationNode) {
        return PresentationParser.extractIssuer(presentationNode);
    }

    /** Returns the credential subject {@code id} from the first verifiable credential in a presentation. */
    default String extractCredentialSubjectId(JsonNode presentationNode) {
        return PresentationParser.extractSubjectId(presentationNode);
    }

    /**
     * Extracts claims from a verified {@code vp_token} according to this definition's DCQL query.
     */
    PresentationClaims extractPresentationClaims(JsonNode vpTokenNode);
}
