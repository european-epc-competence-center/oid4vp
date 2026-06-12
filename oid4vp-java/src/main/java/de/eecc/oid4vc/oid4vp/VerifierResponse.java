package de.eecc.oid4vc.oid4vp;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Builder;

import java.util.List;

/**
 * Response from an external vc-verifier API. Only {@link #verified()} is required.
 */
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
public record VerifierResponse(
        boolean verified,
        VerifierResponse gs1Result,
        VerifierResponse statusResult,
        List<VerifierResponse> results
) {}
