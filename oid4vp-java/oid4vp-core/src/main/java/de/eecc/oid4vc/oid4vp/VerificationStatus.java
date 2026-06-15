package de.eecc.oid4vc.oid4vp;

/**
 * Lifecycle of a {@code vp_token} on an authorization request.
 */
public enum VerificationStatus {

    /** No {@code vp_token} has been posted yet. */
    NOT_RECEIVED,

    /** A {@code vp_token} was posted; cryptographic verification has not finished yet. */
    RECEIVED,

    /** At least one presentation in the {@code vp_token} verified successfully. */
    SUCCEEDED,

    /** A {@code vp_token} was received but verification failed. */
    FAILED
}
