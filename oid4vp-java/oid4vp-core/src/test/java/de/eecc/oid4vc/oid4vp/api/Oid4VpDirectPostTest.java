package de.eecc.oid4vc.oid4vp.api;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.eecc.oid4vc.oid4vp.request.PresentationRequest;
import de.eecc.oid4vc.oid4vp.request.template.gs1.Gs1LicenseRequest;
import de.eecc.oid4vc.oid4vp.verifier.PresentationVerificationResult;
import de.eecc.oid4vc.oid4vp.verifier.PresentationVerifier;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.SuperBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.assertj.core.api.Assertions.assertThat;

class Oid4VpDirectPostTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private Oid4Vp oid4Vp;

    @BeforeEach
    void setUp() {
        oid4Vp = Oid4Vp.builder()
                .options(Oid4VpOptions.builder()
                        .responseUri("https://example.com/api/auth/oid4vp/response")
                        .build())
                .verifier(new AlwaysVerifiedVerifier())
                .objectMapper(MAPPER)
                .build();
    }

    @Test
    void completeOutcome_doesNotIssueResponseCode() {
        PresentationRequest request = oid4Vp.generatePresentationRequest(
                GenerateRequestOptions.<PresentationRequest>builder(Gs1LicenseRequest.INSTANCE)
                        .redirect(false)
                        .build());

        oid4Vp.processDirectPost("{\"cred\":[{\"fake\":\"vp\"}]}", request.getState(),
                (req, node) -> DirectPostResult.complete());

        PresentationRequest stored = oid4Vp.findPresentationRequestByState(request.getState()).orElseThrow();
        assertThat(stored.getResponseCode()).isNull();
        assertThat(stored.isPresentationComplete()).isTrue();
        assertThat(oid4Vp.findPresentationRequestByResponseCode("any")).isEmpty();

        var poll = oid4Vp.pollPresentationStatus(request.getState()).orElseThrow();
        assertThat(poll.completed()).isTrue();
        assertThat(poll.responseCode()).isNull();
    }

    @Test
    void issueResponseCodeOutcome_mintsResponseCode() {
        PresentationRequest request = oid4Vp.generatePresentationRequest(
                GenerateRequestOptions.of(Gs1LicenseRequest.INSTANCE));

        oid4Vp.processDirectPost("{\"cred\":[{\"fake\":\"vp\"}]}", request.getState(),
                (req, node) -> DirectPostResult.issueResponseCode());

        PresentationRequest stored = oid4Vp.findPresentationRequestByState(request.getState()).orElseThrow();
        assertThat(stored.getResponseCode()).isNotBlank();
        assertThat(oid4Vp.pollPresentationStatus(request.getState())).isPresent();
    }

    @Test
    void beforeSave_isInvokedOnceBeforePersistence() {
        AtomicBoolean called = new AtomicBoolean(false);
        PresentationRequest request = oid4Vp.generatePresentationRequest(
                GenerateRequestOptions.<PresentationRequest>builder(Gs1LicenseRequest.INSTANCE)
                        .beforeSave(r -> {
                            assertThat(called.compareAndSet(false, true)).isTrue();
                            r.setRedirect(false);
                        })
                        .build());

        assertThat(called).isTrue();
        assertThat(request.isRedirect()).isFalse();
    }

    @Test
    void builderSupplier_setsSubclassFieldsBeforeSave() {
        TestPresentationRequest request = oid4Vp.generatePresentationRequest(
                GenerateRequestOptions.<TestPresentationRequest>builder(Gs1LicenseRequest.INSTANCE)
                        .builderSupplier(() -> TestPresentationRequest.builder().purpose("ADD_ORG_GCPS"))
                        .build());

        assertThat(request.getPurpose()).isEqualTo("ADD_ORG_GCPS");
    }

    @Getter
    @Setter
    @SuperBuilder
    static class TestPresentationRequest extends PresentationRequest {
        private String purpose;
    }

    private static class AlwaysVerifiedVerifier implements PresentationVerifier {
        @Override
        public PresentationVerificationResult verify(JsonNode presentation, String nonce, String clientId) {
            return PresentationVerificationResult.builder().verified(true).build();
        }

        @Override
        public List<Object> buildVerifierRequestBody(JsonNode presentationNode) {
            return List.of();
        }
    }
}
