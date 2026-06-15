package de.eecc.oid4vc.oid4vp.verifier;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import de.eecc.oid4vc.oid4vp.VerifierResponse;
import de.eecc.oid4vc.oid4vp.exception.InvalidVpToken;
import de.eecc.oid4vc.oid4vp.exception.Oid4VpException;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.Map;

/**
 * HTTP client for external vc-verifier services.
 */
@RequiredArgsConstructor
public class HttpPresentationVerifier implements PresentationVerifier {

    private static final Logger log = LoggerFactory.getLogger(HttpPresentationVerifier.class);

    private final String verifierUrl;
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    public HttpPresentationVerifier(String verifierUrl) {
        this(verifierUrl, HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build(), new ObjectMapper());
    }

    public HttpPresentationVerifier(String verifierUrl, ObjectMapper objectMapper) {
        this(verifierUrl, HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build(), objectMapper);
    }

    @Override
    public PresentationVerificationResult verify(JsonNode presentationNode, String nonce, String clientId) {
        String verifierEndpoint = verifierUrl
                + "?challenge=" + encode(nonce)
                + "&domain=" + encode(clientId);

        log.debug("Calling verifier URL: {}", verifierEndpoint);

        try {
            String body = objectMapper.writeValueAsString(buildVerifierRequestBody(presentationNode));
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(verifierEndpoint))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(body))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            JsonNode responseNode = objectMapper.readTree(response.body());
            VerifierResponse[] verifierResponses = objectMapper.readValue(response.body(), VerifierResponse[].class);
            if (verifierResponses == null || verifierResponses.length == 0) {
                log.warn("Verifier returned empty response");
                return PresentationVerificationResult.builder()
                        .verified(false)
                        .verifierResponse(responseNode)
                        .build();
            }

            boolean verified = verifierResponses[0].verified();
            JsonNode verifierPayload = firstVerifierEntry(responseNode);
            log.debug("Verifier result for nonce={}: verified={}", nonce, verified);
            return PresentationVerificationResult.builder()
                    .verified(verified)
                    .verifierResponse(verifierPayload)
                    .build();
        } catch (Exception e) {
            log.error("Verifier call failed for nonce={}: {}", nonce, e.getMessage(), e);
            JsonNode error = objectMapper.createObjectNode()
                    .put("message", e.getMessage());
            return PresentationVerificationResult.builder()
                    .verified(false)
                    .verifierResponse(error)
                    .build();
        }
    }

    @Override
    public List<Object> buildVerifierRequestBody(JsonNode presentationNode) {
        if (presentationNode.isTextual()) {
            return List.of(presentationNode.asText());
        }
        if (presentationNode.isObject()) {
            return List.of(objectMapper.convertValue(
                    presentationNode, new TypeReference<Map<String, Object>>() {}));
        }
        throw new Oid4VpException(new InvalidVpToken(
                "Unsupported vp_token presentation node type: " + presentationNode.getNodeType()));
    }

    private JsonNode firstVerifierEntry(JsonNode responseNode) {
        if (responseNode == null || responseNode.isNull()) {
            return objectMapper.createObjectNode();
        }
        if (responseNode.isArray()) {
            ArrayNode array = (ArrayNode) responseNode;
            return array.isEmpty() ? responseNode : array.get(0);
        }
        return responseNode;
    }

    private static String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }
}
