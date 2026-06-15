package de.eecc.oid4vc.oid4vp.exception;

public record UnknownState() implements Oid4VpError {

    @Override
    public String message() {
        return "Unknown or expired state";
    }

    @Override
    public int suggestedHttpStatus() {
        return 400;
    }
}
