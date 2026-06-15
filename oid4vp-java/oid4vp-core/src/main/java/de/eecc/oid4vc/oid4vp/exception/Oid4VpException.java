package de.eecc.oid4vc.oid4vp.exception;

/**
 * Unchecked exception for OID4VP processing failures.
 */
public class Oid4VpException extends RuntimeException {

    private final Oid4VpError error;

    public Oid4VpException(Oid4VpError error) {
        super(error.message());
        this.error = error;
    }

    public Oid4VpException(Oid4VpError error, Throwable cause) {
        super(error.message(), cause);
        this.error = error;
    }

    public Oid4VpError error() {
        return error;
    }
}
