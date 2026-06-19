package de.eecc.oid4vc.oid4vp.vp;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class PresentationParserTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Test
    void collectCredentialSubjects_multipleJwtCredentials() throws Exception {
        String vc1 = compactJwtWithPayload("""
                {
                  "credentialSubject": { "licenseValue": "0614141" }
                }
                """);
        String vc2 = compactJwtWithPayload("""
                {
                  "credentialSubject": { "licenseValue": "0614142" }
                }
                """);
        String vpPayloadJson = """
                {
                  "vp": {
                    "verifiableCredential": ["%s", "%s"]
                  }
                }
                """.formatted(vc1, vc2);
        String vpJwt = compactJwtWithPayload(vpPayloadJson);
        JsonNode node = MAPPER.readTree("\"" + vpJwt + "\"");

        List<JsonNode> subjects = PresentationParser.collectCredentialSubjects(node);

        assertThat(subjects).hasSize(2);
        assertThat(subjects.get(0).get("licenseValue").asText()).isEqualTo("0614141");
        assertThat(subjects.get(1).get("licenseValue").asText()).isEqualTo("0614142");
    }

    @Test
    void extractCredentialMetadata_fromLdpPresentation() throws Exception {
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

        assertThat(PresentationParser.extractCredentialType(
                        node, List.of("GS1CompanyPrefixLicenseCredential")))
                .isEqualTo("GS1CompanyPrefixLicenseCredential");
        assertThat(PresentationParser.extractIssuer(node)).isEqualTo("did:example:issuer");
        assertThat(PresentationParser.extractSubjectId(node)).isEqualTo("did:example:holder");
    }

    @Test
    void jwtFromDataUrl_extractsCompactJwt() {
        String dataUrl = "data:application/vc-ld+jwt,eyJhbGciOiJub25lIn0.e30.sig";

        assertThat(PresentationParser.jwtFromDataUrl(dataUrl)).isEqualTo("eyJhbGciOiJub25lIn0.e30.sig");
    }

    private static String compactJwtWithPayload(String jsonPayload) {
        return "eyJhbGciOiJub25lIn0."
                + Base64.getUrlEncoder().withoutPadding()
                        .encodeToString(jsonPayload.getBytes(StandardCharsets.UTF_8))
                + ".sig";
    }
}
