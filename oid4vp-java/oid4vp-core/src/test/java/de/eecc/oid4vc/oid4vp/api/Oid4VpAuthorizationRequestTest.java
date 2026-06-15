package de.eecc.oid4vc.oid4vp.api;

import de.eecc.oid4vc.oid4vp.request.PresentationRequest;
import de.eecc.oid4vc.oid4vp.request.PublicPresentationRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class Oid4VpAuthorizationRequestTest {

    private Oid4Vp oid4Vp;

    @BeforeEach
    void setUp() {
        oid4Vp = Oid4Vp.create(Oid4VpOptions.builder()
                .responseUri("https://example.com/api/auth/oid4vp/response")
                .build());
    }

    @Test
    void toOpenId4VpUrl_fullRequest_includesResponseUri() {
        PresentationRequest request = PresentationRequest.builder()
                .clientId("redirect_uri:https://example.com/api/auth/oid4vp/response")
                .responseUri("https://example.com/api/auth/oid4vp/response")
                .nonce("nonce-value")
                .state("state-value")
                .build();

        String url = oid4Vp.toOpenId4VpUrl(request);

        assertThat(url).startsWith("openid4vp://");
        assertThat(url).contains("response_uri=");
        assertThat(url).doesNotContain("redirect_uri=");
    }

    @Test
    void toOpenId4VpUrl_fullRequest_omitsResponseUriWhenNull() {
        PresentationRequest request = PresentationRequest.builder()
                .clientId("redirect_uri:https://example.com/api/auth/oid4vp/response")
                .nonce("nonce-value")
                .state("state-value")
                .build();

        String url = oid4Vp.toOpenId4VpUrl(request);

        assertThat(url).doesNotContain("response_uri=");
    }

    @Test
    void toOpenId4VpUrl_publicPresentationRequest_includesState() {
        PublicPresentationRequest authorizationRequest = PublicPresentationRequest.builder()
                .clientId("redirect_uri:https://example.com/api/auth/oid4vp/response")
                .requestUri("https://example.com/api/auth/oid4vp/request/abc")
                .state("session-state-123")
                .build();

        String url = oid4Vp.toOpenId4VpUrl(authorizationRequest);

        assertThat(url).contains("state=session-state-123");
        assertThat(url).contains("request_uri=");
        assertThat(url).doesNotContain("redirect_uri=");
    }

    @Test
    void toOpenId4VpUrl_publicPresentationRequest_byValueIncludesRequestParameters() {
        PublicPresentationRequest authorizationRequest = PublicPresentationRequest.builder()
                .clientId("redirect_uri:https://example.com/api/auth/oid4vp/response")
                .state("session-state-123")
                .responseType("vp_token")
                .responseMode("direct_post")
                .responseUri("https://example.com/api/auth/oid4vp/response")
                .nonce("nonce-value")
                .build();

        String url = oid4Vp.toOpenId4VpUrl(authorizationRequest);

        assertThat(url).contains("nonce=nonce-value");
        assertThat(url).contains("response_uri=");
        assertThat(url).contains("state=session-state-123");
        assertThat(url).doesNotContain("request_uri=");
    }
}
