package de.eecc.oid4vc.oid4vp.spring;

import de.eecc.oid4vc.oid4vp.api.Oid4VpOptions;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@ConfigurationProperties("oid4vp")
public record Oid4VpProperties(
        String verifierUrl,
        String responseUri,
        String redirectUri,
        String requestUriBaseUrl,
        boolean requestUriEnabled,
        Duration requestTtl
) {

    public Oid4VpOptions toOptions() {
        return Oid4VpOptions.builder()
                .verifierUrl(verifierUrl)
                .responseUri(responseUri)
                .redirectUri(redirectUri)
                .requestUriBaseUrl(requestUriBaseUrl)
                .requestUriEnabled(requestUriEnabled)
                .requestTtl(requestTtl)
                .build();
    }
}
