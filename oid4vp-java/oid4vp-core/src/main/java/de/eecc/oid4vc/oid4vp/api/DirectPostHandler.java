package de.eecc.oid4vc.oid4vp.api;

import com.fasterxml.jackson.databind.JsonNode;
import de.eecc.oid4vc.oid4vp.request.PresentationRequest;

/**
 * Application hook invoked after a {@code vp_token} has been cryptographically verified.
 *
 * @param <T> authorization request type, including application-specific subclasses
 */
@FunctionalInterface
public interface DirectPostHandler<T extends PresentationRequest> {

    /**
     * @param request  the matched authorization request
     * @param vpToken  parsed {@code vp_token} JSON
     * @return how to finalize direct_post; {@code null} defaults to {@link DirectPostResult#issueResponseCode()}
     */
    DirectPostResult onVerified(T request, JsonNode vpToken);
}
