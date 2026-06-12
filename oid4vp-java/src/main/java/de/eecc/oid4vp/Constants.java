package de.eecc.oid4vp;

import java.util.List;
import java.util.Map;

/**
 * Cross-cutting OpenID4VP constants used throughout the library.
 *
 * @see <a href="https://openid.net/specs/openid-4-verifiable-presentations-1_0.html">OpenID4VP</a>
 */
public final class Constants {

    public static final String RESPONSE_TYPE_VP_TOKEN = "vp_token";
    public static final String RESPONSE_MODE_DIRECT_POST = "direct_post";
    public static final String OPENID4VP_SCHEME = "openid4vp://";
    public static final String CLIENT_ID_REDIRECT_URI_PREFIX = "redirect_uri:";

    /** Verifiable presentation format identifiers (DCQL {@code format} and vp_formats_supported keys). */
    public static final String VP_FORMAT_LDP_VC = "ldp_vc";
    public static final String VP_FORMAT_JWT_VC_JSON = "jwt_vc_json";

    public static final String DATA_URL_PREFIX = "data:";

    public static final int DEFAULT_REQUEST_TTL_SECONDS = 300;

    private Constants() {}

    /**
     * Default {@code vp_formats_supported} for presentation requests.
     */
    public static Map<String, Object> defaultVpFormatsSupported() {
        return Map.of(
                VP_FORMAT_LDP_VC, Map.of(
                        "proof_type_values", List.of("DataIntegrityProof"),
                        "cryptosuite_values", List.of("ecdsa-rdfc-2019")
                ),
                VP_FORMAT_JWT_VC_JSON, Map.of(
                        "alg_values", List.of("ES256")
                )
        );
    }
}
