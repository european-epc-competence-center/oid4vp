package de.eecc.oid4vc.oid4vp.api;

import de.eecc.oid4vc.oid4vp.VpTokenResponse;

/**
 * Result of application logic after cryptographic verification in {@code direct_post}.
 */
public record DirectPostResult(
        DirectPostOutcome outcome,
        VpTokenResponse.DirectPostResponse response
) {

    public static DirectPostResult issueResponseCode() {
        return new DirectPostResult(DirectPostOutcome.ISSUE_RESPONSE_CODE, null);
    }

    public static DirectPostResult complete() {
        return new DirectPostResult(DirectPostOutcome.COMPLETE, VpTokenResponse.DirectPostResponse.empty());
    }

    public static DirectPostResult custom(VpTokenResponse.DirectPostResponse response) {
        return new DirectPostResult(DirectPostOutcome.CUSTOM, response);
    }
}
