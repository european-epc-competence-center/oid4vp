package de.eecc.oid4vc.oid4vp.request;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import de.eecc.oid4vc.oid4vp.ClientMetadata;
import de.eecc.oid4vc.oid4vp.DcqlQuery;
import lombok.Builder;

/**
 * Public view of an OID4VP authorization request for authorize endpoints and frontend polling.
 *
 * <p>When the request is passed by reference, only {@code client_id}, {@code request_uri}, and
 * {@code state} are present. When passed by value (no {@code request_uri}), all authorization
 * request parameters are included per
 * <a href="https://openid.net/specs/openid-4-verifiable-presentations-1_0.html#name-examples">OID4VP §5.4</a>.
 */
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public record PublicPresentationRequest(
        @JsonProperty("client_id") String clientId,
        @JsonProperty("request_uri") String requestUri,
        @JsonProperty("state") String state,
        @JsonProperty("response_type") String responseType,
        @JsonProperty("response_mode") String responseMode,
        @JsonProperty("response_uri") String responseUri,
        @JsonProperty("nonce") String nonce,
        @JsonProperty("dcql_query") DcqlQuery.Query dcqlQuery,
        @JsonProperty("client_metadata") ClientMetadata clientMetadata
) {}
