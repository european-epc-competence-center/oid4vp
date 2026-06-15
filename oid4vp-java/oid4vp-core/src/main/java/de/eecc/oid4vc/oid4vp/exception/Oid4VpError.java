package de.eecc.oid4vc.oid4vp.exception;

/**
 * Typed OID4VP processing errors for host-specific HTTP or domain mapping.
 */
public sealed interface Oid4VpError permits
        UnknownState,
        ExpiredState,
        AlreadyConsumed,
        InvalidVpToken,
        VerificationFailed,
        InternalError {

    String message();

    int suggestedHttpStatus();
}
