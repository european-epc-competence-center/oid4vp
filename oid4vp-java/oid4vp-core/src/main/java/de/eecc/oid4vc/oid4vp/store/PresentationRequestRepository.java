package de.eecc.oid4vc.oid4vp.store;

import de.eecc.oid4vc.oid4vp.request.PresentationRequest;

import java.util.Optional;

/**
 * Persistence for OID4VP authorization requests.
 *
 * <p>Implementations index requests by state, nonce, request id, and optionally response code.
 */
public interface PresentationRequestRepository {

    /**
     * Persists a newly created authorization request (indexed by nonce and state).
     */
    <T extends PresentationRequest> void save(T request);

    Optional<PresentationRequest> findByNonce(String nonce);

    Optional<PresentationRequest> findByState(String state);

    Optional<PresentationRequest> findByRequestId(String requestId);

    Optional<PresentationRequest> findByResponseCode(String responseCode);

    /**
     * Registers a {@code request_uri} transport id and indexes the request by request id.
     *
     * @return the generated request id
     */
    <T extends PresentationRequest> String registerRequestUri(T request);

    /**
     * Marks the pending nonce index as consumed and removes nonce/request-id lookups (replay protection).
     */
    void invalidatePending(String nonce);

    /**
     * After successful direct_post with a one-time response code: index by code and refresh state entry.
     */
    <T extends PresentationRequest> void saveAfterDirectPost(T request);

    /**
     * After successful direct_post without a response code: refresh state entry only.
     */
    <T extends PresentationRequest> void saveAfterComplete(T request);

    /**
     * Persists verification failure so poll endpoints can report the error.
     */
    <T extends PresentationRequest> void saveAfterVerificationFailure(T request);

    /**
     * Drops the one-time {@code response_code} index after the code has been consumed.
     */
    <T extends PresentationRequest> void invalidateResponseCode(T request);
}
