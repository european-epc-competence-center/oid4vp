package de.eecc.oid4vc.oid4vp.exception;

public record VerificationFailed() implements Oid4VpError {

    @Override
    public String message() {
        return "No valid verifiable presentation found in vp_token";
    }

    @Override
    public int suggestedHttpStatus() {
        return 401;
    }
}
