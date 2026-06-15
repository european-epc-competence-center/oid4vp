package de.eecc.oid4vc.oid4vp.api;

/**
 * How {@link Oid4Vp#processDirectPost} should finalize a successful verification.
 */
public enum DirectPostOutcome {

    /** Mint a one-time response code and optionally build a redirect URI. */
    ISSUE_RESPONSE_CODE,

    /** Verification succeeded without issuing a response code. */
    COMPLETE,

    /** Handler supplied the full direct_post response; library skips default redirect logic. */
    CUSTOM
}
