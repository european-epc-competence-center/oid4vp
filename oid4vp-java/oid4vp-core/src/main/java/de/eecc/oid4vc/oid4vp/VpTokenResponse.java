package de.eecc.oid4vc.oid4vp;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonValue;
import tools.jackson.databind.JsonNode;
import lombok.Builder;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * OID4VP direct_post and polling response shapes.
 */
public final class VpTokenResponse {

    private VpTokenResponse() {}

    /**
     * OID4VP §8.1 {@code vp_token}: a JSON object whose keys are DCQL credential query ids and whose
     * values are arrays of presentations. The object is the token. It is not wrapped in another field.
     */
    public record VpToken(Map<String, List<Object>> presentations) {
        @JsonCreator
        public VpToken {
            presentations = presentations == null ? Map.of() : Map.copyOf(presentations);
        }

        /** The §8.1 object itself. */
        @JsonValue
        public Map<String, List<Object>> presentations() {
            return presentations;
        }

        /** One presentation for each credential query id. {@code multiple} false means a single entry. */
        public static VpToken oneEach(Object presentation, List<String> credentialQueryIds) {
            Map<String, List<Object>> byQueryId = new LinkedHashMap<>();
            if (credentialQueryIds != null) {
                for (String id : credentialQueryIds) {
                    if (id != null && !id.isBlank()) {
                        byQueryId.computeIfAbsent(id, key -> new ArrayList<>()).add(presentation);
                    }
                }
            }
            return new VpToken(byQueryId);
        }
    }

    /** Response returned to the wallet after processing direct_post. */
    @Builder
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record DirectPostResponse(@JsonProperty("redirect_uri") String redirectUri) {

        public static DirectPostResponse empty() {
            return DirectPostResponse.builder().build();
        }

        public static DirectPostResponse redirect(String redirectUri) {
            return DirectPostResponse.builder().redirectUri(redirectUri).build();
        }
    }

    @Builder
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record PollResponse(
            @JsonProperty("response_code") String responseCode,
            Boolean completed,
            @JsonProperty("vp_token_received") Boolean vpTokenReceived,
            @JsonProperty("verified") Boolean verified,
            @JsonProperty("verification_error") JsonNode verificationError
    ) {
        public static PollResponse withResponseCode(String responseCode) {
            return PollResponse.builder()
                    .responseCode(responseCode)
                    .vpTokenReceived(true)
                    .verified(true)
                    .build();
        }

        public static PollResponse asCompleted() {
            return PollResponse.builder()
                    .completed(true)
                    .vpTokenReceived(true)
                    .verified(true)
                    .build();
        }

        public static PollResponse verificationFailed(JsonNode verificationError) {
            return PollResponse.builder()
                    .vpTokenReceived(true)
                    .verified(false)
                    .verificationError(verificationError)
                    .build();
        }
    }
}
