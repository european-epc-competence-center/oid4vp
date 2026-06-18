package de.eecc.oid4vc.oid4vp.verifier;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.eecc.oid4vc.oid4vp.exception.Oid4VpException;
import de.eecc.oid4vc.oid4vp.exception.VerificationFailed;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class VerifierResponseErrorsTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Test
    void extractMessage_readsTopLevelErrorName() throws Exception {
        JsonNode response = MAPPER.readTree("""
                {
                  "verified": false,
                  "error": {
                    "name": "Presentation holder does not match credential subject."
                  }
                }
                """);

        assertThat(VerifierResponseErrors.extractMessage(response))
                .isEqualTo("Presentation holder does not match credential subject.");
    }

    @Test
    void extractMessage_readsPresentationResultErrorName() throws Exception {
        JsonNode response = MAPPER.readTree("""
                {
                  "presentationResult": {
                    "verified": false,
                    "error": {
                      "name": "Nested presentation error."
                    }
                  },
                  "verified": false
                }
                """);

        assertThat(VerifierResponseErrors.extractMessage(response))
                .isEqualTo("Nested presentation error.");
    }

    @Test
    void extractMessage_prefersTopLevelErrorOverPresentationResult() throws Exception {
        JsonNode response = MAPPER.readTree("""
                {
                  "presentationResult": {
                    "error": {
                      "name": "Nested error."
                    }
                  },
                  "error": {
                    "name": "Top-level error."
                  }
                }
                """);

        assertThat(VerifierResponseErrors.extractMessage(response))
                .isEqualTo("Top-level error.");
    }

    @Test
    void extractMessage_readsHttpFailureMessage() throws Exception {
        JsonNode response = MAPPER.readTree("""
                {
                  "message": "Connection refused"
                }
                """);

        assertThat(VerifierResponseErrors.extractMessage(response))
                .isEqualTo("Connection refused");
    }

    @Test
    void extractMessage_returnsNullForMissingError() throws Exception {
        JsonNode response = MAPPER.readTree("""
                {
                  "verified": false
                }
                """);

        assertThat(VerifierResponseErrors.extractMessage(response)).isNull();
    }

    @Test
    void verificationFailed_usesVerifierErrorWhenPresent() throws Exception {
        JsonNode response = MAPPER.readTree("""
                {
                  "verified": false,
                  "error": {
                    "name": "Presentation holder \\"did:example:holder\\" does not match credential subject \\"did:example:subject\\"."
                  }
                }
                """);

        Oid4VpException exception = new Oid4VpException(VerificationFailed.fromVerifierResponse(response));

        assertThat(exception.getMessage())
                .isEqualTo("Presentation holder \"did:example:holder\" does not match credential subject \"did:example:subject\".");
        assertThat(exception.error()).isInstanceOf(VerificationFailed.class);
    }

    @Test
    void verificationFailed_fallsBackToDefaultMessage() {
        Oid4VpException exception = new Oid4VpException(VerificationFailed.fromVerifierResponse(null));

        assertThat(exception.getMessage())
                .isEqualTo("No valid verifiable presentation found in vp_token");
    }
}
