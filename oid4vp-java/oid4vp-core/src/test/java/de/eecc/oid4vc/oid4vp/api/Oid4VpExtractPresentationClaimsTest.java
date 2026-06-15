package de.eecc.oid4vc.oid4vp.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import de.eecc.oid4vc.oid4vp.PresentationClaims;
import de.eecc.oid4vc.oid4vp.exception.EmptyPresentationClaims;
import de.eecc.oid4vc.oid4vp.exception.InvalidVpToken;
import de.eecc.oid4vc.oid4vp.exception.Oid4VpException;
import de.eecc.oid4vc.oid4vp.request.PresentationRequest;
import de.eecc.oid4vc.oid4vp.request.template.gs1.Gs1LicenseRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class Oid4VpExtractPresentationClaimsTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private Oid4Vp oid4Vp;

    @BeforeEach
    void setUp() {
        oid4Vp = Oid4Vp.builder()
                .options(Oid4VpOptions.builder()
                        .responseUri("https://example.com/api/auth/oid4vp/response")
                        .build())
                .objectMapper(MAPPER)
                .build();
    }

    @Test
    void extractPresentationClaims_fromStoredRequest() throws Exception {
        PresentationRequest request = oid4Vp.generatePresentationRequest(Gs1LicenseRequest.INSTANCE);
        request.setVpToken("""
                {
                  "gs1_license": [{
                    "verifiableCredential": [{
                      "type": ["VerifiableCredential", "GS1CompanyPrefixLicenseCredential"],
                      "credentialSubject": {
                        "licenseValue": "0614141",
                        "organization": {
                          "gs1:organizationName": "ACME",
                          "gs1:partyGLN": "9501234567890"
                        }
                      }
                    }]
                  }]
                }
                """);

        PresentationClaims claims = oid4Vp.extractPresentationClaims(Gs1LicenseRequest.INSTANCE, request);

        assertThat(claims.values()).containsExactly("0614141");
        assertThat(claims.name()).isEqualTo("ACME");
        assertThat(claims.identifier()).isEqualTo("9501234567890");
    }

    @Test
    void extractPresentationClaims_emptyValues_throwsEmptyPresentationClaims() {
        assertThatThrownBy(() -> oid4Vp.extractPresentationClaims(
                        Gs1LicenseRequest.INSTANCE, "{\"gs1_license\": [{\"verifiableCredential\": []}]}"))
                .isInstanceOf(Oid4VpException.class)
                .satisfies(ex -> {
                    Oid4VpException oid4VpException = (Oid4VpException) ex;
                    assertThat(oid4VpException.error()).isInstanceOf(EmptyPresentationClaims.class);
                    assertThat(oid4VpException.error().suggestedHttpStatus()).isEqualTo(401);
                });
    }

    @Test
    void extractPresentationClaims_missingVpToken_throwsInvalidVpToken() {
        PresentationRequest request = oid4Vp.generatePresentationRequest(Gs1LicenseRequest.INSTANCE);

        assertThatThrownBy(() -> oid4Vp.extractPresentationClaims(Gs1LicenseRequest.INSTANCE, request))
                .isInstanceOf(Oid4VpException.class)
                .satisfies(ex -> assertThat(((Oid4VpException) ex).error()).isInstanceOf(InvalidVpToken.class));
    }

    @Test
    void extractPresentationClaims_invalidJson_throwsInvalidVpToken() {
        assertThatThrownBy(() -> oid4Vp.extractPresentationClaims(Gs1LicenseRequest.INSTANCE, "not-json"))
                .isInstanceOf(Oid4VpException.class)
                .satisfies(ex -> assertThat(((Oid4VpException) ex).error()).isInstanceOf(InvalidVpToken.class));
    }
}
