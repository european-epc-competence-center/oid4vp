package de.eecc.oid4vc.oid4vp.api;

import de.eecc.oid4vc.oid4vp.Constants;
import lombok.Builder;

import java.time.Duration;

/**
 * Runtime configuration for {@link Oid4Vp}.
 */
@Builder
public record Oid4VpOptions(
        @Builder.Default String verifierUrl,
        String responseUri,
        String redirectUri,
        String requestUriBaseUrl,
        boolean requestUriEnabled,
        @Builder.Default Duration requestTtl,
        @Builder.Default Duration authorizationRequestTtl
) {

    private static final String DEFAULT_VERIFIER_URL = "http://vc-verifier:3000/api/verifier";
    private static final Duration DEFAULT_REQUEST_TTL =
            Duration.ofSeconds(Constants.DEFAULT_REQUEST_TTL_SECONDS);
    private static final Duration DEFAULT_AUTHORIZATION_REQUEST_TTL = Duration.ofMinutes(5);

    public Oid4VpOptions {
        if (verifierUrl == null) {
            verifierUrl = DEFAULT_VERIFIER_URL;
        }
        if (requestTtl == null || requestTtl.isZero() || requestTtl.isNegative()) {
            requestTtl = DEFAULT_REQUEST_TTL;
        }
        if (authorizationRequestTtl == null || authorizationRequestTtl.isZero()
                || authorizationRequestTtl.isNegative()) {
            authorizationRequestTtl = DEFAULT_AUTHORIZATION_REQUEST_TTL;
        }
    }
}
