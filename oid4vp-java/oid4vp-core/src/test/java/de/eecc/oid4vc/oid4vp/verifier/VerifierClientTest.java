package de.eecc.oid4vc.oid4vp.verifier;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.eecc.oid4vc.oid4vp.api.Oid4Vp;
import de.eecc.oid4vc.oid4vp.api.Oid4VpOptions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * vc-verifier POST body: {@code ["JWT_STRING", { ... }]} — strings quoted, objects not.
 */
class VerifierClientTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private Oid4Vp oid4Vp;

    @BeforeEach
    void setUp() {
        oid4Vp = Oid4Vp.create(Oid4VpOptions.builder().build());
    }

    @Test
    void verifierRequestBody_serializesJwtAsQuotedStringAndLdpAsObject() throws Exception {
        String jwt = "eyJhbGciOiJIUzI1NiJ9.eyJ0eXAiOiJIUzI1NiJ9.sig";
        JsonNode vpToken = MAPPER.readTree("""
                {
                  "credential": [
                    "%s",
                    {
                      "@context": ["https://www.w3.org/2018/credentials/v1"],
                      "type": ["VerifiablePresentation"]
                    }
                  ]
                }
                """.formatted(jwt));

        JsonNode presentations = vpToken.get("credential");
        List<Object> verifierBody = new ArrayList<>();
        verifierBody.addAll(oid4Vp.buildVerifierRequestBody(presentations.get(0)));
        verifierBody.addAll(oid4Vp.buildVerifierRequestBody(presentations.get(1)));

        String arrayJson = MAPPER.writeValueAsString(verifierBody);
        assertThat(arrayJson).isEqualTo(
                "[\"" + jwt + "\",{\"@context\":[\"https://www.w3.org/2018/credentials/v1\"],"
                        + "\"type\":[\"VerifiablePresentation\"]}]");
    }

    @Test
    void buildVerifierRequestBody_jwtPresentation_isStringElement() throws Exception {
        String jwt = "eyJhbGciOiJIUzI1NiJ9.eyJ0eXAiOiJIUzI1NiJ9.sig";
        JsonNode node = MAPPER.readTree("\"" + jwt + "\"");

        List<Object> body = oid4Vp.buildVerifierRequestBody(node);

        assertThat(body).hasSize(1);
        assertThat(body.getFirst()).isInstanceOf(String.class);
        assertThat(body.getFirst()).isEqualTo(jwt);
    }

    @Test
    void buildVerifierRequestBody_ldpPresentation_isObjectElement() throws Exception {
        JsonNode node = MAPPER.readTree("""
                {
                  "@context": ["https://www.w3.org/2018/credentials/v1"],
                  "type": ["VerifiablePresentation"],
                  "proof": {
                    "type": "DataIntegrityProof",
                    "challenge": "n-0S6_WzA2Mj",
                    "domain": "redirect_uri:https://example.com/cb"
                  }
                }
                """);

        List<Object> body = oid4Vp.buildVerifierRequestBody(node);

        assertThat(body).hasSize(1);
        assertThat(body.getFirst()).isInstanceOf(Map.class);
        @SuppressWarnings("unchecked")
        Map<String, Object> vp = (Map<String, Object>) body.getFirst();
        assertThat(vp).containsKey("@context");
        assertThat(vp.get("type")).isEqualTo(List.of("VerifiablePresentation"));
    }
}
