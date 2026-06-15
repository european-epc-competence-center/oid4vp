package de.eecc.oid4vc.oid4vp.request;

import de.eecc.oid4vc.oid4vp.VerificationStatus;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.SuperBuilder;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class PresentationRequestTest {

    @Getter
    @Setter
    @SuperBuilder
    static class CustomPresentationRequest extends PresentationRequest {
        private String purpose;
        private Long organizationId;
    }

    @Test
    void subclassStoresApplicationSpecificValues() {
        CustomPresentationRequest request = CustomPresentationRequest.builder()
                .state("state-1")
                .purpose("LOGIN")
                .organizationId(42L)
                .build();

        assertThat(request.getPurpose()).isEqualTo("LOGIN");
        assertThat(request.getOrganizationId()).isEqualTo(42L);
    }

    @Test
    void setVpTokenUpdatesVerificationStatus() {
        PresentationRequest request = PresentationRequest.builder().state("state-1").build();

        assertThat(request.getVerificationStatus()).isEqualTo(VerificationStatus.NOT_RECEIVED);

        request.setVpToken("{\"credential\":[]}");
        assertThat(request.getVerificationStatus()).isEqualTo(VerificationStatus.RECEIVED);

        request.setVerificationStatus(VerificationStatus.FAILED);
        assertThat(request.getVerificationStatus()).isEqualTo(VerificationStatus.FAILED);

        request.setVpToken(null);
        assertThat(request.getVerificationStatus()).isEqualTo(VerificationStatus.NOT_RECEIVED);
    }

    @Test
    void publicPresentationRequestIncludesWireParametersWhenRequestUriMissing() {
        PresentationRequest request = PresentationRequest.builder()
                .clientId("client")
                .responseUri("https://example.com/response")
                .nonce("nonce-1")
                .state("state-1")
                .build();

        PublicPresentationRequest authorizationRequest = request.publicPresentationRequest();

        assertThat(authorizationRequest.clientId()).isEqualTo("client");
        assertThat(authorizationRequest.state()).isEqualTo("state-1");
        assertThat(authorizationRequest.requestUri()).isNull();
        assertThat(authorizationRequest.responseType()).isEqualTo("vp_token");
        assertThat(authorizationRequest.responseMode()).isEqualTo("direct_post");
        assertThat(authorizationRequest.responseUri()).isEqualTo("https://example.com/response");
        assertThat(authorizationRequest.nonce()).isEqualTo("nonce-1");
    }
}
