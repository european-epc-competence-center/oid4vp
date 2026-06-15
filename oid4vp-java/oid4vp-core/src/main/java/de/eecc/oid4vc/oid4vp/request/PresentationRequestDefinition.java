package de.eecc.oid4vc.oid4vp.request;

import com.fasterxml.jackson.databind.JsonNode;
import de.eecc.oid4vc.oid4vp.ClientMetadata;
import de.eecc.oid4vc.oid4vp.DcqlQuery;
import de.eecc.oid4vc.oid4vp.PresentationClaims;

/**
 * Credential-specific parts of an OID4VP authorization request.
 * Transport fields (nonce, state, client_id, response_uri) are supplied by {@link de.eecc.oid4vc.oid4vp.api.Oid4Vp}.
 */
public interface PresentationRequestDefinition {

    DcqlQuery.Query dcqlQuery();

    default ClientMetadata clientMetadata() {
        return ClientMetadata.presentationDefault();
    }

    /**
     * Extracts claims from a verified {@code vp_token} according to this definition's DCQL query.
     */
    PresentationClaims extractPresentationClaims(JsonNode vpTokenNode);
}
