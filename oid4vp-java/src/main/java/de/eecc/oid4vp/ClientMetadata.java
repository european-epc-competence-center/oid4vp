package de.eecc.oid4vp;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;

/**
 * OpenID4VP {@code client_metadata} parameter.
 */
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ClientMetadata(
        @JsonProperty("vp_formats_supported") Object vpFormatsSupported
) {

    public static ClientMetadata presentationDefault() {
        return ClientMetadata.builder()
                .vpFormatsSupported(Constants.defaultVpFormatsSupported())
                .build();
    }
}
