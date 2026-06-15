package de.eecc.oid4vc.oid4vp;

import java.util.List;
import java.util.Map;

/**
 * Credential-agnostic claims extracted from a verified OID4VP {@code vp_token}. Retrieves the claims requested by the presentation definition in a generic way.
 */
public interface PresentationClaims {

    /** Primary organisation or subject identifier when present. */
    String identifier();

    /** Human-readable name when present. */
    String name();

    /** Primary multi-valued claim requested by the presentation definition. */
    List<String> values();

    /** Verifiable-credential type when known. */
    default String credentialType() {
        return null;
    }

    /** Claim values keyed by DCQL / presentation claim id. */
    Map<String, Object> claimValues();
}
