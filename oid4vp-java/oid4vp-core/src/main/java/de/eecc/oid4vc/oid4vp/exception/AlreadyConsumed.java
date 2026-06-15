package de.eecc.oid4vc.oid4vp.exception;

public record AlreadyConsumed() implements Oid4VpError {

    @Override
    public String message() {
        return "Request already consumed";
    }

    @Override
    public int suggestedHttpStatus() {
        return 400;
    }
}
