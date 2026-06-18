package de.eecc.oid4vc.oid4vp.exception;

import com.fasterxml.jackson.databind.JsonNode;
import de.eecc.oid4vc.oid4vp.verifier.VerifierResponseErrors;

public record VerificationFailed(String detail) implements Oid4VpError {

    private static final String DEFAULT_MESSAGE = "No valid verifiable presentation found in vp_token";

    public static VerificationFailed fromVerifierResponse(JsonNode verifierResponse) {
        String message = VerifierResponseErrors.extractMessage(verifierResponse);
        return message != null ? new VerificationFailed(message) : new VerificationFailed(DEFAULT_MESSAGE);
    }

    @Override
    public String message() {
        return detail != null && !detail.isBlank() ? detail : DEFAULT_MESSAGE;
    }

    @Override
    public int suggestedHttpStatus() {
        return 401;
    }
}
