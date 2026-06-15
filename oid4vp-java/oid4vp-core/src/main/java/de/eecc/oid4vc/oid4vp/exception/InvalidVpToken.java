package de.eecc.oid4vc.oid4vp.exception;

public record InvalidVpToken(String detail) implements Oid4VpError {

    @Override
    public String message() {
        return detail;
    }

    @Override
    public int suggestedHttpStatus() {
        return 400;
    }
}
