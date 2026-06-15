package de.eecc.oid4vc.oid4vp.verifier;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.Builder;

/**
 * Outcome of a single presentation verification call to an external vc-verifier.
 *
 * @param verified          whether the verifier reported success
 * @param verifierResponse  raw verifier payload for the presentation, or an error object
 */
@Builder
public record PresentationVerificationResult(boolean verified, JsonNode verifierResponse) {}
