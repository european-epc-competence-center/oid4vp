package de.eecc.oid4vc.oid4vp.exception;

public record EmptyPresentationClaims() implements Oid4VpError {

    @Override
    public String message() {
        return "Verified presentation does not contain the requested primary claim values";
    }

    @Override
    public int suggestedHttpStatus() {
        return 401;
    }
}
