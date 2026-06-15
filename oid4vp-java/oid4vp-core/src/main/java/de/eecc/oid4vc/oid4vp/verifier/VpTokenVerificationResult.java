package de.eecc.oid4vc.oid4vp.verifier;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.Builder;

/**
 * Aggregate verification outcome for all presentations in a {@code vp_token}.
 *
 * @param anyVerified whether at least one presentation verified successfully
 * @param firstError  verifier payload for the first failed presentation, if any
 */
@Builder
public record VpTokenVerificationResult(boolean anyVerified, JsonNode firstError) {}
