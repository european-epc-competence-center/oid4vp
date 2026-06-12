package de.eecc.oid4vc.oid4vp;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;

import java.util.List;
import java.util.Map;

/**
 * Digital Credentials Query Language (DCQL) models per OpenID4VP Section 6.
 */
public final class DcqlQuery {

    private DcqlQuery() {}

    @Builder
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record Query(
            @JsonProperty("credentials") List<CredentialQuery> credentials
    ) {}

    @Builder
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record CredentialQuery(
            @JsonProperty("id") String id,
            @JsonProperty("format") String format,
            @JsonProperty("meta") Map<String, Object> meta,
            @JsonProperty("claims") List<ClaimsQuery> claims,
            @JsonProperty("require_cryptographic_holder_binding")
            Boolean requireCryptographicHolderBinding
    ) {
        public CredentialQuery {
            if (requireCryptographicHolderBinding == null) {
                requireCryptographicHolderBinding = true;
            }
        }
    }

    @Builder
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record ClaimsQuery(
            @JsonProperty("id") String id,
            @JsonProperty("path") List<Object> path
    ) {}
}
