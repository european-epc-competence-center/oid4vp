package de.eecc.oid4vc.oid4vp.api;

import de.eecc.oid4vc.oid4vp.request.PresentationRequest;
import de.eecc.oid4vc.oid4vp.VpTokenResponse;

import java.util.Optional;

/**
 * Customizes poll status for a matched authorization request.
 */
@FunctionalInterface
public interface PollStatusResolver<T extends PresentationRequest> {

    Optional<VpTokenResponse.PollResponse> resolve(T request);
}
