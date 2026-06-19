package de.eecc.oid4vc.oid4vp.vp;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.eecc.oid4vc.oid4vp.Constants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.function.Function;

/**
 * Format-agnostic helpers for parsing OID4VP {@code vp_token} presentations.
 */
public final class PresentationParser {

    private static final Logger log = LoggerFactory.getLogger(PresentationParser.class);
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private PresentationParser() {}

    public static boolean isCompactJwt(String value) {
        if (value == null || value.isBlank()) {
            return false;
        }
        int firstDot = value.indexOf('.');
        if (firstDot <= 0) {
            return false;
        }
        int secondDot = value.indexOf('.', firstDot + 1);
        return secondDot > firstDot + 1 && secondDot < value.length() - 1;
    }

    public static String jwtFromDataUrl(String id) {
        if (id == null || !id.startsWith(Constants.DATA_URL_PREFIX)) {
            return null;
        }
        int comma = id.indexOf(',');
        if (comma < 0 || comma == id.length() - 1) {
            return null;
        }
        String mediaType = id.substring(Constants.DATA_URL_PREFIX.length(), comma);
        if (!mediaType.contains("jwt")) {
            return null;
        }
        return id.substring(comma + 1);
    }

    public static JsonNode parseJwtPayload(String compactJwt) {
        try {
            String[] parts = compactJwt.split("\\.", 3);
            if (parts.length != 3) {
                return null;
            }
            byte[] decoded = Base64.getUrlDecoder().decode(parts[1]);
            return MAPPER.readTree(decoded);
        } catch (IllegalArgumentException | java.io.IOException e) {
            log.warn("Failed to parse JWT presentation payload: {}", e.getMessage());
            return null;
        }
    }

    public static JsonNode presentationRoot(JsonNode presentationNode) {
        if (presentationNode == null || presentationNode.isMissingNode()) {
            return null;
        }
        if (presentationNode.isTextual() && isCompactJwt(presentationNode.asText())) {
            return parseJwtPayload(presentationNode.asText());
        }
        return presentationNode;
    }

    public static JsonNode findCredentialSubject(JsonNode presentationNode) {
        JsonNode root = presentationRoot(presentationNode);
        if (root == null || root.isMissingNode()) {
            return null;
        }

        JsonNode direct = root.get("credentialSubject");
        if (direct != null && !direct.isMissingNode()) {
            return direct;
        }

        JsonNode subject = credentialSubjectFromVerifiableCredentials(root.get("verifiableCredential"));
        if (subject != null) {
            return subject;
        }

        JsonNode vp = root.get("vp");
        if (vp != null && vp.isObject()) {
            return credentialSubjectFromVerifiableCredentials(vp.get("verifiableCredential"));
        }

        return null;
    }

    public static List<JsonNode> collectCredentialSubjects(JsonNode presentationNode) {
        JsonNode root = presentationRoot(presentationNode);
        if (root == null || root.isMissingNode()) {
            return List.of();
        }

        JsonNode direct = root.get("credentialSubject");
        if (direct != null && !direct.isMissingNode()) {
            return List.of(direct);
        }

        List<JsonNode> subjects = credentialSubjectsFromVerifiableCredentials(root.get("verifiableCredential"));
        if (!subjects.isEmpty()) {
            return subjects;
        }

        JsonNode vp = root.get("vp");
        if (vp != null && vp.isObject()) {
            return credentialSubjectsFromVerifiableCredentials(vp.get("verifiableCredential"));
        }

        return List.of();
    }

    /**
     * Returns the specific credential type from the first matching verifiable credential in a presentation.
     *
     * @param acceptedTypes when non-empty, only types in this list are returned; otherwise the first
     *                      type other than {@code VerifiableCredential} / {@code VerifiablePresentation}
     */
    public static String extractCredentialType(JsonNode presentationNode, List<String> acceptedTypes) {
        return fromFirstCredential(
                presentationNode, entry -> credentialTypeFromCredentialEntry(entry, acceptedTypes));
    }

    /** Returns the issuer from the first verifiable credential in a presentation. */
    public static String extractIssuer(JsonNode presentationNode) {
        return fromFirstCredential(presentationNode, PresentationParser::issuerFromCredentialEntry);
    }

    /** Returns the credential subject {@code id} from the first verifiable credential in a presentation. */
    public static String extractSubjectId(JsonNode presentationNode) {
        JsonNode root = presentationRoot(presentationNode);
        if (root != null && !root.isMissingNode()) {
            JsonNode directSubject = root.get("credentialSubject");
            if (directSubject != null && !directSubject.isMissingNode()) {
                String subjectId = textClaim(directSubject.get("id"));
                if (subjectId != null) {
                    return subjectId;
                }
            }
        }
        return fromFirstCredential(presentationNode, PresentationParser::subjectIdFromCredentialEntry);
    }

    private static String fromFirstCredential(JsonNode presentationNode, Function<JsonNode, String> extractor) {
        JsonNode root = presentationRoot(presentationNode);
        if (root == null || root.isMissingNode()) {
            return null;
        }

        String value = fromVerifiableCredentials(root.get("verifiableCredential"), extractor);
        if (value != null) {
            return value;
        }

        JsonNode vp = root.get("vp");
        if (vp != null && vp.isObject()) {
            value = fromVerifiableCredentials(vp.get("verifiableCredential"), extractor);
            if (value != null) {
                return value;
            }
        }

        return extractor.apply(root);
    }

    private static String fromVerifiableCredentials(
            JsonNode verifiableCredential, Function<JsonNode, String> extractor) {
        if (verifiableCredential == null || !verifiableCredential.isArray()) {
            return null;
        }

        for (JsonNode credentialEntry : verifiableCredential) {
            String value = extractor.apply(credentialEntry);
            if (value != null) {
                return value;
            }
        }

        return null;
    }

    private static String credentialTypeFromCredentialEntry(JsonNode credentialEntry, List<String> acceptedTypes) {
        if (credentialEntry == null || credentialEntry.isMissingNode()) {
            return null;
        }

        if (credentialEntry.isTextual()) {
            String compactJwt = compactJwtFromTextualCredential(credentialEntry.asText());
            if (compactJwt == null) {
                return null;
            }
            return credentialTypeFromJwtPayload(parseJwtPayload(compactJwt), acceptedTypes);
        }

        if (!credentialEntry.isObject()) {
            return null;
        }

        String type = specificTypeFromTypeArray(credentialEntry.get("type"), acceptedTypes);
        if (type != null) {
            return type;
        }

        String compactJwt = extractEnvelopedJwtFromCredential(credentialEntry);
        if (compactJwt == null) {
            return null;
        }

        return credentialTypeFromJwtPayload(parseJwtPayload(compactJwt), acceptedTypes);
    }

    private static String credentialTypeFromJwtPayload(JsonNode payload, List<String> acceptedTypes) {
        if (payload == null || payload.isMissingNode()) {
            return null;
        }

        String type = specificTypeFromTypeArray(payload.get("type"), acceptedTypes);
        if (type != null) {
            return type;
        }

        JsonNode nestedVc = payload.get("vc");
        if (nestedVc != null && nestedVc.isObject()) {
            type = specificTypeFromTypeArray(nestedVc.get("type"), acceptedTypes);
            if (type != null) {
                return type;
            }
        }

        JsonNode nestedCredential = payload.get("credential");
        if (nestedCredential != null && nestedCredential.isObject()) {
            return specificTypeFromTypeArray(nestedCredential.get("type"), acceptedTypes);
        }

        return null;
    }

    private static String specificTypeFromTypeArray(JsonNode typeNode, List<String> acceptedTypes) {
        if (typeNode == null || !typeNode.isArray()) {
            return null;
        }

        for (JsonNode entry : typeNode) {
            if (!entry.isTextual()) {
                continue;
            }
            String value = entry.asText();
            if (acceptedTypes != null && !acceptedTypes.isEmpty()) {
                if (acceptedTypes.contains(value)) {
                    return value;
                }
                continue;
            }
            if (!"VerifiableCredential".equals(value) && !"VerifiablePresentation".equals(value)) {
                return value;
            }
        }

        return null;
    }

    private static String issuerFromCredentialEntry(JsonNode credentialEntry) {
        if (credentialEntry == null || credentialEntry.isMissingNode()) {
            return null;
        }

        if (credentialEntry.isTextual()) {
            String compactJwt = compactJwtFromTextualCredential(credentialEntry.asText());
            if (compactJwt == null) {
                return null;
            }
            return issuerFromJson(parseJwtPayload(compactJwt));
        }

        if (!credentialEntry.isObject()) {
            return null;
        }

        String issuer = issuerFromJson(credentialEntry);
        if (issuer != null) {
            return issuer;
        }

        String compactJwt = extractEnvelopedJwtFromCredential(credentialEntry);
        if (compactJwt == null) {
            return null;
        }

        return issuerFromJson(parseJwtPayload(compactJwt));
    }

    private static String issuerFromJson(JsonNode node) {
        if (node == null || node.isMissingNode()) {
            return null;
        }

        String issuer = textClaim(node.get("issuer"));
        if (issuer != null) {
            return issuer;
        }

        JsonNode issuerNode = node.get("issuer");
        if (issuerNode != null && issuerNode.isObject()) {
            issuer = textClaim(issuerNode.get("id"));
            if (issuer != null) {
                return issuer;
            }
        }

        return textClaim(node.get("iss"));
    }

    private static String subjectIdFromCredentialEntry(JsonNode credentialEntry) {
        JsonNode subject = credentialSubjectFromVerifiableCredentialEntry(credentialEntry);
        if (subject == null || subject.isMissingNode()) {
            return null;
        }
        return textClaim(subject.get("id"));
    }

    private static String textClaim(JsonNode value) {
        if (value == null || value.isNull() || !value.isTextual()) {
            return null;
        }
        String text = value.asText().trim();
        return text.isBlank() ? null : text;
    }

    public static String compactJwtFromTextualCredential(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String trimmed = value.trim();
        String candidate = isCompactJwt(trimmed) ? trimmed : jwtFromDataUrl(trimmed);
        return candidate != null && isCompactJwt(candidate) ? candidate : null;
    }

    public static String extractEnvelopedJwtFromCredential(JsonNode credentialEntry) {
        if (credentialEntry == null || !credentialEntry.isObject()) {
            return null;
        }

        JsonNode id = credentialEntry.get("id");
        if (id != null && id.isTextual()) {
            String compactJwt = compactJwtFromTextualCredential(id.asText());
            if (compactJwt != null) {
                return compactJwt;
            }
        }

        JsonNode jwt = credentialEntry.get("jwt");
        if (jwt != null && jwt.isTextual()) {
            return compactJwtFromTextualCredential(jwt.asText());
        }

        return null;
    }

    private static JsonNode credentialSubjectFromVerifiableCredentials(JsonNode verifiableCredential) {
        List<JsonNode> subjects = credentialSubjectsFromVerifiableCredentials(verifiableCredential);
        if (!subjects.isEmpty()) {
            return subjects.getFirst();
        }

        if (verifiableCredential == null || !verifiableCredential.isArray() || verifiableCredential.isEmpty()) {
            return null;
        }

        for (JsonNode credentialEntry : verifiableCredential) {
            JsonNode subject = credentialSubjectFromVerifiableCredentialEntry(credentialEntry);
            if (subject != null && !subject.isMissingNode()) {
                return subject;
            }
        }

        return null;
    }

    private static List<JsonNode> credentialSubjectsFromVerifiableCredentials(JsonNode verifiableCredential) {
        if (verifiableCredential == null || !verifiableCredential.isArray() || verifiableCredential.isEmpty()) {
            return List.of();
        }

        List<JsonNode> subjects = new ArrayList<>();
        for (JsonNode credentialEntry : verifiableCredential) {
            JsonNode subject = credentialSubjectFromVerifiableCredentialEntry(credentialEntry);
            if (subject != null && !subject.isMissingNode()) {
                subjects.add(subject);
            }
        }

        return subjects;
    }

    private static JsonNode credentialSubjectFromVerifiableCredentialEntry(JsonNode credentialEntry) {
        if (credentialEntry == null || credentialEntry.isMissingNode()) {
            return null;
        }

        if (credentialEntry.isTextual()) {
            String compactJwt = compactJwtFromTextualCredential(credentialEntry.asText());
            if (compactJwt == null) {
                return null;
            }
            JsonNode vcPayload = parseJwtPayload(compactJwt);
            return credentialSubjectFromJwtVcPayload(vcPayload);
        }

        if (!credentialEntry.isObject()) {
            return null;
        }

        JsonNode direct = credentialEntry.get("credentialSubject");
        if (direct != null && !direct.isMissingNode() && !direct.isNull()) {
            return direct;
        }

        String compactJwt = extractEnvelopedJwtFromCredential(credentialEntry);
        if (compactJwt == null) {
            return null;
        }

        JsonNode vcPayload = parseJwtPayload(compactJwt);
        if (vcPayload == null) {
            return null;
        }

        return credentialSubjectFromJwtVcPayload(vcPayload);
    }

    private static JsonNode credentialSubjectFromJwtVcPayload(JsonNode payload) {
        if (payload == null || payload.isMissingNode()) {
            return null;
        }

        JsonNode direct = payload.get("credentialSubject");
        if (direct != null && !direct.isMissingNode() && !direct.isNull()) {
            return direct;
        }

        JsonNode nestedVc = payload.get("vc");
        if (nestedVc != null && nestedVc.isObject()) {
            JsonNode nestedSubject = nestedVc.get("credentialSubject");
            if (nestedSubject != null && !nestedSubject.isMissingNode() && !nestedSubject.isNull()) {
                return nestedSubject;
            }
        }

        JsonNode nestedCredential = payload.get("credential");
        if (nestedCredential != null && nestedCredential.isObject()) {
            JsonNode nestedSubject = nestedCredential.get("credentialSubject");
            if (nestedSubject != null && !nestedSubject.isMissingNode() && !nestedSubject.isNull()) {
                return nestedSubject;
            }
        }

        return null;
    }
}
