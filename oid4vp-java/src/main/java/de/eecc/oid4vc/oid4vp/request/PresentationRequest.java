package de.eecc.oid4vc.oid4vp.request;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import de.eecc.oid4vc.oid4vp.ClientMetadata;
import de.eecc.oid4vc.oid4vp.Constants;
import de.eecc.oid4vc.oid4vp.DcqlQuery;
import de.eecc.oid4vc.oid4vp.VerificationStatus;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

import java.time.Instant;

/**
 * OID4VP authorization request from creation through response handling.
 * Serialized for wallets and authorize endpoints; lifecycle fields are omitted from JSON.
 *
 * <p>Application-specific attributes (for example a custom {@code purpose} or target
 * organisation id) belong on a subclass and are never exposed on the wire.
 */
@Getter
@Setter
@SuperBuilder
public class PresentationRequest {

    @JsonProperty("response_type")
    @Builder.Default
    private String responseType = Constants.RESPONSE_TYPE_VP_TOKEN;

    @JsonProperty("response_mode")
    @Builder.Default
    private String responseMode = Constants.RESPONSE_MODE_DIRECT_POST;

    @JsonProperty("client_id")
    private String clientId;

    @JsonProperty("response_uri")
    private String responseUri;

    @JsonProperty("nonce")
    private String nonce;

    @JsonProperty("state")
    private String state;

    @JsonProperty("dcql_query")
    private DcqlQuery.Query dcqlQuery;

    @JsonProperty("client_metadata")
    private ClientMetadata clientMetadata;

    @JsonIgnore
    private Instant createdAt;

    @JsonIgnore
    private Instant expiresAt;

    @JsonIgnore
    private boolean consumed;

    @JsonIgnore
    private String requestUri;

    @JsonIgnore
    private String requestId;

    @JsonIgnore
    private String responseCode;

    @JsonIgnore
    private String vpToken;

    /** When {@code false}, {@code direct_post} omits a user-agent redirect after success. */
    @JsonIgnore
    @Builder.Default
    private boolean redirect = true;

    @JsonIgnore
    @Builder.Default
    private VerificationStatus verificationStatus = VerificationStatus.NOT_RECEIVED;

    @JsonIgnore
    private JsonNode verificationError;

    public void setVpToken(String vpToken) {
        this.vpToken = vpToken;
        if (vpToken != null && !vpToken.isBlank()) {
            verificationStatus = VerificationStatus.RECEIVED;
        } else {
            verificationStatus = VerificationStatus.NOT_RECEIVED;
        }
    }

    public PublicPresentationRequest publicPresentationRequest() {
        if (requestUri != null) {
            return PublicPresentationRequest.builder()
                    .clientId(clientId)
                    .requestUri(requestUri)
                    .state(state)
                    .build();
        }
        return PublicPresentationRequest.builder()
                .clientId(clientId)
                .state(state)
                .responseType(responseType)
                .responseMode(responseMode)
                .responseUri(responseUri)
                .nonce(nonce)
                .dcqlQuery(dcqlQuery)
                .clientMetadata(clientMetadata)
                .build();
    }
}
