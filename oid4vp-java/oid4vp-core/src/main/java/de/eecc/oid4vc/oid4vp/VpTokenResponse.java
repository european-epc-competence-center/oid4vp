package de.eecc.oid4vc.oid4vp;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.Builder;

import java.util.List;
import java.util.Map;

/**
 * OID4VP direct_post and polling response shapes.
 */
public final class VpTokenResponse {

    private VpTokenResponse() {}

    /** Parsed vp_token JSON object: credential query id → presentation arrays. */
    @Builder
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record VpToken(Map<String, List<Object>> presentations) {}

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
