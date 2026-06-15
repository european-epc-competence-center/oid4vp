package de.eecc.oid4vc.oid4vp.exception;

public record ExpiredState() implements Oid4VpError {

    @Override
    public String message() {
        return "Request expired";
    }

    @Override
    public int suggestedHttpStatus() {
        return 400;
    }
}
