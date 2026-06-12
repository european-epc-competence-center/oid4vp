package de.eecc.oid4vc.oid4vp.store;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import de.eecc.oid4vc.oid4vp.request.PresentationRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.security.SecureRandom;
import java.time.Duration;
import java.util.Base64;
import java.util.Optional;

/**
 * Transient store for OID4VP authorization requests.
 */
public class PresentationRequestStore {

    private static final Logger log = LoggerFactory.getLogger(PresentationRequestStore.class);

    private final Cache<String, PresentationRequest> byNonce;
    private final Cache<String, PresentationRequest> byState;
    private final Cache<String, PresentationRequest> byRequestId;
    private final Cache<String, PresentationRequest> byResponseCode;
    private final SecureRandom secureRandom = new SecureRandom();

    public PresentationRequestStore(Duration ttl) {
        Duration effectiveTtl = ttl.isZero() || ttl.isNegative() ? Duration.ofSeconds(1) : ttl;
        this.byNonce = newCache(effectiveTtl);
        this.byState = newCache(effectiveTtl);
        this.byRequestId = newCache(effectiveTtl);
        this.byResponseCode = newCache(effectiveTtl);
    }

    private static Cache<String, PresentationRequest> newCache(Duration ttl) {
        return Caffeine.newBuilder()
                .expireAfterWrite(ttl)
                .maximumSize(10_000)
                .build();
    }

    public <T extends PresentationRequest> void save(T request) {
        byNonce.put(request.getNonce(), request);
        byState.put(request.getState(), request);
        log.debug("Saved OID4VP request nonce={} state={}", request.getNonce(), request.getState());
    }

    public Optional<PresentationRequest> findByNonce(String nonce) {
        return Optional.ofNullable(byNonce.getIfPresent(nonce));
    }

    public Optional<PresentationRequest> findByState(String state) {
        return Optional.ofNullable(byState.getIfPresent(state));
    }

    public Optional<PresentationRequest> findByRequestId(String requestId) {
        return Optional.ofNullable(byRequestId.getIfPresent(requestId));
    }

    public Optional<PresentationRequest> findByResponseCode(String responseCode) {
        return Optional.ofNullable(byResponseCode.getIfPresent(responseCode));
    }

    public <T extends PresentationRequest> String registerRequestUri(T request) {
        String requestId = generateRequestId();
        request.setRequestId(requestId);
        byRequestId.put(requestId, request);
        log.debug("Registered OID4VP request_uri id={} state={}", requestId, request.getState());
        return requestId;
    }

    public void invalidatePending(String nonce) {
        PresentationRequest request = byNonce.getIfPresent(nonce);
        if (request != null) {
            request.setConsumed(true);
            byNonce.invalidate(nonce);
            byState.invalidate(request.getState());
            if (request.getRequestId() != null) {
                byRequestId.invalidate(request.getRequestId());
            }
            log.debug("Invalidated pending OID4VP request nonce={}", nonce);
        }
    }

    public <T extends PresentationRequest> void saveAfterDirectPost(T request) {
        byResponseCode.put(request.getResponseCode(), request);
        byState.put(request.getState(), request);
        log.debug("Saved OID4VP request after direct_post state={} responseCode={}",
                request.getState(), request.getResponseCode());
    }

    public <T extends PresentationRequest> void saveAfterVerificationFailure(T request) {
        byState.put(request.getState(), request);
        log.debug("Saved OID4VP verification failure state={}", request.getState());
    }

    /**
     * Drops the one-time {@code response_code} index. Call after the code has been consumed
     * (for example at a token endpoint); redemption tracking itself is application-specific.
     */
    public <T extends PresentationRequest> void invalidateResponseCode(T request) {
        if (request.getResponseCode() != null) {
            byResponseCode.invalidate(request.getResponseCode());
            request.setResponseCode(null);
            byState.put(request.getState(), request);
            log.debug("Invalidated OID4VP response_code for state={}", request.getState());
        }
    }

    private String generateRequestId() {
        byte[] bytes = new byte[24];
        secureRandom.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}
