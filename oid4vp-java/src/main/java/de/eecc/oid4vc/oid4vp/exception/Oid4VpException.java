package de.eecc.oid4vc.oid4vp.exception;

/**
 * Unchecked exception for OID4VP processing failures.
 */
public class Oid4VpException extends RuntimeException {

    private final int httpStatus;

    public Oid4VpException(int httpStatus, String message) {
        super(message);
        this.httpStatus = httpStatus;
    }

    public Oid4VpException(int httpStatus, String message, Throwable cause) {
        super(message, cause);
        this.httpStatus = httpStatus;
    }

    public int httpStatus() {
        return httpStatus;
    }
}
