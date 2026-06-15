package de.eecc.oid4vc.oid4vp.verifier;

import com.fasterxml.jackson.databind.JsonNode;

import java.util.List;

/**
 * Pluggable presentation verification against an external vc-verifier service.
 */
public interface PresentationVerifier {

    /**
     * Verifies a single presentation from a {@code vp_token}.
     *
     * @param presentation presentation node (string JWT or object)
     * @param nonce        expected challenge
     * @param clientId     expected audience (domain)
     */
    PresentationVerificationResult verify(JsonNode presentation, String nonce, String clientId);

    /**
     * Builds the vc-verifier POST body for a single presentation.
     */
    List<Object> buildVerifierRequestBody(JsonNode presentationNode);
}
