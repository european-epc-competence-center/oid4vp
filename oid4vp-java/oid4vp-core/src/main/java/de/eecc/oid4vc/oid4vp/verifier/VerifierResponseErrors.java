package de.eecc.oid4vc.oid4vp.verifier;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * Extracts human-readable error messages from vc-verifier API responses.
 */
public final class VerifierResponseErrors {

    private VerifierResponseErrors() {}

    /**
     * Returns the first non-blank error message found in a verifier response payload, or {@code null}.
     */
    public static String extractMessage(JsonNode response) {
        if (response == null || response.isNull()) {
            return null;
        }

        String message = extractFromErrorNode(response.get("error"));
        if (message != null) {
            return message;
        }

        message = textAt(response, "message");
        if (message != null) {
            return message;
        }

        JsonNode presentationResult = response.get("presentationResult");
        if (presentationResult != null && !presentationResult.isNull()) {
            message = extractFromErrorNode(presentationResult.get("error"));
            if (message != null) {
                return message;
            }
        }

        return null;
    }

    private static String extractFromErrorNode(JsonNode error) {
        if (error == null || error.isNull()) {
            return null;
        }
        String name = textAt(error, "name");
        if (name != null) {
            return name;
        }
        return textAt(error, "message");
    }

    private static String textAt(JsonNode node, String field) {
        JsonNode value = node.get(field);
        if (value != null && value.isTextual()) {
            String text = value.asText();
            return text.isBlank() ? null : text;
        }
        return null;
    }
}
