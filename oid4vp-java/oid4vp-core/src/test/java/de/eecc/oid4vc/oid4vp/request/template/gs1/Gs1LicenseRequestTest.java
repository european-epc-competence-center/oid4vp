package de.eecc.oid4vc.oid4vp.request.template.gs1;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.eecc.oid4vc.oid4vp.Constants;
import de.eecc.oid4vc.oid4vp.DcqlQuery;
import de.eecc.oid4vc.oid4vp.PresentationClaims;
import de.eecc.oid4vc.oid4vp.api.Oid4Vp;
import de.eecc.oid4vc.oid4vp.api.Oid4VpOptions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class Gs1LicenseRequestTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private Oid4Vp oid4Vp;

    @BeforeEach
    void setUp() {
        oid4Vp = Oid4Vp.create(Oid4VpOptions.builder()
                .responseUri("https://example.com/api/auth/oid4vp/response")
                .build());
    }

    @Test
    void dcqlQuery_requestsGs1LicenseCredentialsInBothFormats() {
        DcqlQuery.Query query = Gs1LicenseRequest.INSTANCE.templateDcqlQuery();

        assertThat(query.credentials()).hasSize(2);
        assertThat(query.credentials())
                .extracting(DcqlQuery.CredentialQuery::id)
                .containsOnly(Gs1LicenseRequest.CREDENTIAL_ID);
        assertThat(query.credentials())
                .extracting(DcqlQuery.CredentialQuery::format)
                .containsExactly(
                        Constants.VP_FORMAT_LDP_VC,
                        Constants.VP_FORMAT_JWT_VC_JSON);
        assertThat(query.credentials().getFirst().claims())
                .extracting(DcqlQuery.ClaimsQuery::id)
                .containsExactly(
                        Gs1LicenseRequest.CLAIM_LICENSE_VALUE,
                        Gs1LicenseRequest.CLAIM_ORGANIZATION_NAME,
                        Gs1LicenseRequest.CLAIM_PARTY_GLN);
    }

    @Test
    void generatePresentationRequest_usesGs1Definition() {
        var request = oid4Vp.generatePresentationRequest(Gs1LicenseRequest.INSTANCE);

        assertThat(request.getDcqlQuery()).isEqualTo(Gs1LicenseRequest.INSTANCE.dcqlQuery());
        assertThat(oid4Vp.toOpenId4VpUrl(request)).contains("dcql_query=");
    }

    @Test
    void generatePresentationRequest_appliesIdentityConstraints() {
        var definition = Gs1LicenseRequest.INSTANCE
                .requiresIssuer("did:example:issuer")
                .requiresSubjectId("did:example:holder");
        var request = oid4Vp.generatePresentationRequest(definition);

        assertThat(request.getDcqlQuery()).isEqualTo(definition.dcqlQuery());
        assertThat(request.getDcqlQuery().credentials().getFirst().issuers())
                .containsExactly("did:example:issuer");
        assertThat(request.getDcqlQuery().credentials().getFirst().subjectIds())
                .containsExactly("did:example:holder");
    }

    @Test
    void extractLicenseValues_ldpPresentation() throws Exception {
        JsonNode node = MAPPER.readTree("""
                {
                  "type": ["VerifiablePresentation"],
                  "verifiableCredential": [{
                    "credentialSubject": {
                      "licenseValue": "0614141"
                    }
                  }]
                }
                """);

        assertThat(Gs1LicenseRequest.extractLicenseValues(node)).containsExactly("0614141");
    }

    @Test
    void extractLicenseValues_multipleCredentialsInOnePresentation() throws Exception {
        JsonNode node = MAPPER.readTree("""
                {
                  "type": ["VerifiablePresentation"],
                  "verifiableCredential": [
                    {
                      "type": ["VerifiableCredential", "GS1CompanyPrefixLicenseCredential"],
                      "credentialSubject": { "licenseValue": "0614141" }
                    },
                    {
                      "type": ["VerifiableCredential", "GS1CompanyPrefixLicenseCredential"],
                      "credentialSubject": { "licenseValue": "0614142" }
                    }
                  ]
                }
                """);

        assertThat(Gs1LicenseRequest.extractLicenseValues(node)).containsExactly("0614141", "0614142");
    }

    @Test
    void extractLicenseValues_jwtVpPresentation() throws Exception {
        String payloadJson = """
                {
                  "vp": {
                    "verifiableCredential": [{
                      "credentialSubject": { "licenseValue": "095100" }
                    }]
                  }
                }
                """;
        String jwt = "eyJhbGciOiJub25lIn0."
                + Base64.getUrlEncoder().withoutPadding()
                        .encodeToString(payloadJson.getBytes(StandardCharsets.UTF_8))
                + ".sig";
        JsonNode node = MAPPER.readTree("\"" + jwt + "\"");

        assertThat(Gs1LicenseRequest.extractLicenseValues(node)).containsExactly("095100");
    }

    @Test
    void extractLicenseValues_gs1ExpandedLicenseValueClaim() throws Exception {
        JsonNode node = MAPPER.readTree("""
                {
                  "type": ["VerifiablePresentation"],
                  "verifiableCredential": [
                    {
                      "credentialSubject": { "licenseValue": "0614141" }
                    },
                    {
                      "credentialSubject": { "gs1:licenseValue": "0614142" }
                    }
                  ]
                }
                """);

        assertThat(Gs1LicenseRequest.extractLicenseValues(node)).containsExactly("0614141", "0614142");
    }

    @Test
    void extractCredentialType_companyPrefixLicense() throws Exception {
        JsonNode node = MAPPER.readTree("""
                {
                  "type": ["VerifiablePresentation"],
                  "verifiableCredential": [{
                    "type": ["VerifiableCredential", "GS1CompanyPrefixLicenseCredential"],
                    "issuer": "did:example:issuer",
                    "credentialSubject": {
                      "id": "did:example:holder",
                      "licenseValue": "0614141"
                    }
                  }]
                }
                """);

        assertThat(Gs1LicenseRequest.INSTANCE.extractCredentialType(node))
                .isEqualTo(Gs1LicenseRequest.TYPE_COMPANY_PREFIX);
        assertThat(Gs1LicenseRequest.INSTANCE.extractCredentialIssuer(node))
                .isEqualTo("did:example:issuer");
        assertThat(Gs1LicenseRequest.INSTANCE.extractCredentialSubjectId(node))
                .isEqualTo("did:example:holder");
    }

    @Test
    void extractPresentationClaims_includesOrganizationAndCredentialType() throws Exception {
        JsonNode vpToken = MAPPER.readTree("""
                {
                  "gs1_license": [{
                    "type": ["VerifiablePresentation"],
                    "verifiableCredential": [{
                      "type": ["VerifiableCredential", "GS1PrefixLicenseCredential"],
                      "credentialSubject": {
                        "licenseValue": "095100",
                        "organization": {
                          "gs1:organizationName": "Acme GS1",
                          "gs1:partyGLN": "9501100000000"
                        }
                      }
                    }]
                  }]
                }
                """);

        PresentationClaims claims = Gs1LicenseRequest.INSTANCE.extractPresentationClaims(vpToken);

        assertThat(claims.values()).containsExactly("095100");
        assertThat(claims.identifier()).isEqualTo("9501100000000");
        assertThat(claims.name()).isEqualTo("Acme GS1");
        assertThat(claims.credentialType()).isEqualTo(Gs1LicenseRequest.TYPE_PREFIX);
    }

    @Test
    void extractPresentationClaims_multipleCredentialsInVpToken() throws Exception {
        JsonNode vpToken = MAPPER.readTree("""
                {
                  "gs1_license": [{
                    "type": ["VerifiablePresentation"],
                    "verifiableCredential": [
                      { "credentialSubject": { "licenseValue": "095100" } },
                      { "credentialSubject": { "gs1:licenseValue": "40000951" } }
                    ]
                  }]
                }
                """);

        PresentationClaims claims = Gs1LicenseRequest.INSTANCE.extractPresentationClaims(vpToken);

        assertThat(claims.values()).containsExactly("095100", "40000951");
    }
}
