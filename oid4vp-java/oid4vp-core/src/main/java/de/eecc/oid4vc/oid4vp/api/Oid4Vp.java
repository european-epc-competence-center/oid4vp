package de.eecc.oid4vc.oid4vp.api;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.eecc.oid4vc.oid4vp.Constants;
import de.eecc.oid4vc.oid4vp.request.PresentationRequest;
import de.eecc.oid4vc.oid4vp.request.PresentationRequestDefinition;
import de.eecc.oid4vc.oid4vp.request.PublicPresentationRequest;
import de.eecc.oid4vc.oid4vp.VerificationStatus;
import de.eecc.oid4vc.oid4vp.VpTokenResponse;
import de.eecc.oid4vc.oid4vp.exception.AlreadyConsumed;
import de.eecc.oid4vc.oid4vp.exception.ExpiredState;
import de.eecc.oid4vc.oid4vp.exception.InternalError;
import de.eecc.oid4vc.oid4vp.exception.InvalidVpToken;
import de.eecc.oid4vc.oid4vp.exception.Oid4VpException;
import de.eecc.oid4vc.oid4vp.exception.UnknownState;
import de.eecc.oid4vc.oid4vp.exception.VerificationFailed;
import de.eecc.oid4vc.oid4vp.store.PresentationRequestRepository;
import de.eecc.oid4vc.oid4vp.verifier.PresentationVerifier;
import de.eecc.oid4vc.oid4vp.verifier.VpTokenVerificationResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.Map;
import java.util.Optional;

/**
 * Main entry point for the OpenID4VP Java library.
 */
public final class Oid4Vp {

    private static final Logger log = LoggerFactory.getLogger(Oid4Vp.class);

    private final Oid4VpOptions options;
    private final PresentationRequestRepository repository;
    private final PresentationVerifier verifier;
    private final ObjectMapper objectMapper;
    private final SecureRandom secureRandom;

    Oid4Vp(Oid4VpOptions options, PresentationRequestRepository repository, PresentationVerifier verifier,
            ObjectMapper objectMapper, SecureRandom secureRandom) {
        this.options = options;
        this.repository = repository;
        this.verifier = verifier;
        this.objectMapper = objectMapper;
        this.secureRandom = secureRandom;
    }

    public static Oid4VpBuilder builder() {
        return new Oid4VpBuilder();
    }

    public static Oid4Vp create(Oid4VpOptions options) {
        return builder().options(options).build();
    }

    public PresentationRequest generatePresentationRequest(PresentationRequestDefinition definition) {
        return generatePresentationRequest(GenerateRequestOptions.of(definition));
    }

    public <T extends PresentationRequest> T generatePresentationRequest(GenerateRequestOptions<T> generateOptions) {
        T request = buildAuthorizationRequest(generateOptions);
        if (generateOptions.beforeSave() != null) {
            generateOptions.beforeSave().accept(request);
        }
        repository.save(request);
        if (options.requestUriEnabled()) {
            String requestId = repository.registerRequestUri(request);
            request.setRequestUri(options.requestUriBaseUrl() + "/request/" + requestId);
        }
        return request;
    }

    @SuppressWarnings("unchecked")
    private <T extends PresentationRequest> T buildAuthorizationRequest(GenerateRequestOptions<T> generateOptions) {
        PresentationRequestDefinition definition = generateOptions.definition();
        String nonce = generateNonce();
        String state = generateNonce();
        String clientId = generateOptions.redirect()
                ? Constants.CLIENT_ID_REDIRECT_URI_PREFIX + options.responseUri()
                : options.responseUri();
        Instant now = Instant.now();

        PresentationRequest.PresentationRequestBuilder<?, ?> builder = generateOptions.builderSupplier().get();
        builder.clientId(clientId)
                .responseUri(options.responseUri())
                .nonce(nonce)
                .state(state)
                .dcqlQuery(definition.dcqlQuery())
                .clientMetadata(definition.clientMetadata())
                .createdAt(now)
                .expiresAt(now.plus(options.authorizationRequestTtl()))
                .redirect(generateOptions.redirect());
        return (T) builder.build();
    }

    public String toOpenId4VpUrl(PublicPresentationRequest authorizationRequest) {
        if (authorizationRequest.requestUri() != null) {
            StringBuilder builder = new StringBuilder(Constants.OPENID4VP_SCHEME)
                    .append("?client_id=").append(encode(authorizationRequest.clientId()))
                    .append("&response_type=").append(encode(Constants.RESPONSE_TYPE_VP_TOKEN))
                    .append("&response_mode=").append(encode(Constants.RESPONSE_MODE_DIRECT_POST))
                    .append("&request_uri=").append(encode(authorizationRequest.requestUri()));
            if (authorizationRequest.state() != null) {
                builder.append("&state=").append(encode(authorizationRequest.state()));
            }
            return builder.toString();
        }
        try {
            StringBuilder builder = new StringBuilder(Constants.OPENID4VP_SCHEME)
                    .append("?client_id=").append(encode(authorizationRequest.clientId()))
                    .append("&response_type=").append(encode(authorizationRequest.responseType()))
                    .append("&response_mode=").append(encode(authorizationRequest.responseMode()))
                    .append("&nonce=").append(encode(authorizationRequest.nonce()));
            if (authorizationRequest.state() != null) {
                builder.append("&state=").append(encode(authorizationRequest.state()));
            }
            if (authorizationRequest.responseUri() != null) {
                builder.append("&response_uri=").append(encode(authorizationRequest.responseUri()));
            }
            if (authorizationRequest.dcqlQuery() != null) {
                builder.append("&dcql_query=")
                        .append(encode(objectMapper.writeValueAsString(authorizationRequest.dcqlQuery())));
            }
            if (authorizationRequest.clientMetadata() != null) {
                builder.append("&client_metadata=")
                        .append(encode(objectMapper.writeValueAsString(authorizationRequest.clientMetadata())));
            }
            return builder.toString();
        } catch (JsonProcessingException e) {
            throw new Oid4VpException(new InternalError(
                    "Failed to serialize request to openid4vp URL: " + e.getMessage()), e);
        }
    }

    public <T extends PresentationRequest> String toOpenId4VpUrl(T request) {
        try {
            StringBuilder builder = new StringBuilder(Constants.OPENID4VP_SCHEME)
                    .append("?client_id=").append(encode(request.getClientId()))
                    .append("&response_type=").append(encode(request.getResponseType()))
                    .append("&response_mode=").append(encode(request.getResponseMode()))
                    .append("&nonce=").append(encode(request.getNonce()))
                    .append("&state=").append(encode(request.getState()));
            if (request.getResponseUri() != null) {
                builder.append("&response_uri=").append(encode(request.getResponseUri()));
            }
            if (request.getDcqlQuery() != null) {
                builder.append("&dcql_query=").append(encode(objectMapper.writeValueAsString(request.getDcqlQuery())));
            }
            if (request.getClientMetadata() != null) {
                builder.append("&client_metadata=")
                        .append(encode(objectMapper.writeValueAsString(request.getClientMetadata())));
            }
            return builder.toString();
        } catch (JsonProcessingException e) {
            throw new Oid4VpException(new InternalError(
                    "Failed to serialize request to openid4vp URL: " + e.getMessage()), e);
        }
    }

    public Optional<PresentationRequest> findPresentationRequestById(String requestId) {
        return repository.findByRequestId(requestId);
    }

    public <T extends PresentationRequest> Optional<T> findPresentationRequestById(String requestId, Class<T> type) {
        return findPresentationRequestById(requestId).filter(type::isInstance).map(type::cast);
    }

    public Optional<PresentationRequest> findPresentationRequestByState(String state) {
        return repository.findByState(state)
                .filter(request -> !Instant.now().isAfter(request.getExpiresAt()));
    }

    public <T extends PresentationRequest> Optional<T> findPresentationRequestByState(String state, Class<T> type) {
        return findPresentationRequestByState(state).filter(type::isInstance).map(type::cast);
    }

    public Optional<PresentationRequest> findPresentationRequestByResponseCode(String responseCode) {
        return repository.findByResponseCode(responseCode);
    }

    public <T extends PresentationRequest> Optional<T> findPresentationRequestByResponseCode(
            String responseCode, Class<T> type) {
        return findPresentationRequestByResponseCode(responseCode).filter(type::isInstance).map(type::cast);
    }

    public Optional<VpTokenResponse.PollResponse> pollPresentationStatus(String state) {
        return pollPresentationStatus(state, defaultPollResolver());
    }

    @SuppressWarnings("unchecked")
    public <T extends PresentationRequest> Optional<VpTokenResponse.PollResponse> pollPresentationStatus(
            String state, PollStatusResolver<T> resolver) {
        return findPresentationRequestByState(state)
                .flatMap(request -> {
                    T typed = (T) request;
                    Optional<VpTokenResponse.PollResponse> custom = resolver.resolve(typed);
                    if (custom.isPresent()) {
                        return custom;
                    }
                    return Optional.ofNullable(defaultPollResponse(request));
                });
    }

    private static VpTokenResponse.PollResponse defaultPollResponse(PresentationRequest request) {
        if (request.getVerificationStatus() == VerificationStatus.FAILED) {
            return VpTokenResponse.PollResponse.verificationFailed(request.getVerificationError());
        }
        if (request.getResponseCode() != null && !request.getResponseCode().isBlank()) {
            return VpTokenResponse.PollResponse.withResponseCode(request.getResponseCode());
        }
        if (request.isPresentationComplete()) {
            return VpTokenResponse.PollResponse.asCompleted();
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    private <T extends PresentationRequest> PollStatusResolver<T> defaultPollResolver() {
        return request -> Optional.empty();
    }

    public <T extends PresentationRequest> VpTokenResponse.DirectPostResponse processDirectPost(
            String vpToken,
            String state,
            DirectPostHandler<T> handler) {
        log.info("Processing OID4VP direct_post for state={}", state);

        PresentationRequest request = findPresentationRequestByState(state)
                .orElseThrow(() -> new Oid4VpException(new UnknownState()));

        if (request.isConsumed()) {
            throw new Oid4VpException(new AlreadyConsumed());
        }
        if (Instant.now().isAfter(request.getExpiresAt())) {
            throw new Oid4VpException(new ExpiredState());
        }

        request.setVpToken(vpToken);
        JsonNode vpTokenNode = parseVpToken(vpToken);

        VpTokenVerificationResult verification = verifyVpTokenPresentations(
                vpTokenNode, request.getNonce(), request.getClientId());
        if (!verification.anyVerified()) {
            request.setVerificationStatus(VerificationStatus.FAILED);
            request.setVerificationError(verification.firstError());
            repository.saveAfterVerificationFailure(request);
            throw new Oid4VpException(new VerificationFailed());
        }

        request.setVerificationStatus(VerificationStatus.SUCCEEDED);
        @SuppressWarnings("unchecked")
        T typedRequest = (T) request;
        DirectPostResult result = handler.onVerified(typedRequest, vpTokenNode);
        if (result == null) {
            result = DirectPostResult.issueResponseCode();
        }

        repository.invalidatePending(request.getNonce());

        return switch (result.outcome()) {
            case ISSUE_RESPONSE_CODE -> finalizeWithResponseCode(request, result.response());
            case COMPLETE -> finalizeComplete(request, result.response());
            case CUSTOM -> finalizeCustom(request, result.response());
        };
    }

    private VpTokenResponse.DirectPostResponse finalizeWithResponseCode(
            PresentationRequest request, VpTokenResponse.DirectPostResponse handlerResponse) {
        request.setResponseCode(generateNonce());
        repository.saveAfterDirectPost(request);
        log.info("OID4VP direct_post succeeded for state={}, issued response_code", request.getState());

        if (request.isRedirect()
                && (handlerResponse == null || handlerResponse.redirectUri() == null)
                && options.redirectUri() != null) {
            return VpTokenResponse.DirectPostResponse.redirect(
                    options.redirectUri() + "#response_code=" + request.getResponseCode());
        }

        return handlerResponse != null ? handlerResponse : VpTokenResponse.DirectPostResponse.empty();
    }

    private VpTokenResponse.DirectPostResponse finalizeComplete(
            PresentationRequest request, VpTokenResponse.DirectPostResponse handlerResponse) {
        request.setPresentationComplete(true);
        repository.saveAfterComplete(request);
        log.info("OID4VP direct_post completed for state={} without response_code", request.getState());
        return handlerResponse != null ? handlerResponse : VpTokenResponse.DirectPostResponse.empty();
    }

    private VpTokenResponse.DirectPostResponse finalizeCustom(
            PresentationRequest request, VpTokenResponse.DirectPostResponse handlerResponse) {
        request.setResponseCode(generateNonce());
        repository.saveAfterDirectPost(request);
        log.info("OID4VP direct_post succeeded for state={} with custom response", request.getState());
        return handlerResponse != null ? handlerResponse : VpTokenResponse.DirectPostResponse.empty();
    }

    /**
     * Invalidates the one-time {@code response_code} after it has been consumed.
     */
    public <T extends PresentationRequest> void invalidateResponseCode(T request) {
        repository.invalidateResponseCode(request);
    }

    public VpTokenVerificationResult verifyVpTokenPresentations(JsonNode vpTokenNode, String nonce, String clientId) {
        boolean anyVerified = false;
        JsonNode firstError = null;

        for (Map.Entry<String, JsonNode> entry : vpTokenNode.properties()) {
            JsonNode presentations = entry.getValue();
            if (!presentations.isArray()) {
                log.warn("VP token entry for {} is not an array", entry.getKey());
                continue;
            }

            for (JsonNode presentationNode : presentations) {
                log.debug("Verifying presentation for credentialId={}", entry.getKey());
                var result = verifier.verify(presentationNode, nonce, clientId);
                if (result.verified()) {
                    anyVerified = true;
                } else if (firstError == null) {
                    firstError = result.verifierResponse();
                }
            }
        }

        return VpTokenVerificationResult.builder()
                .anyVerified(anyVerified)
                .firstError(firstError)
                .build();
    }

    public java.util.List<Object> buildVerifierRequestBody(JsonNode presentationNode) {
        return verifier.buildVerifierRequestBody(presentationNode);
    }

    private JsonNode parseVpToken(String vpToken) {
        try {
            return objectMapper.readTree(vpToken);
        } catch (JsonProcessingException e) {
            throw new Oid4VpException(new InvalidVpToken("Invalid vp_token JSON: " + e.getMessage()), e);
        }
    }

    private String generateNonce() {
        byte[] bytes = new byte[24];
        secureRandom.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private static String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }
}
