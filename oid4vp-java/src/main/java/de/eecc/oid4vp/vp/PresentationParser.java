package de.eecc.oid4vp.vp;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.eecc.oid4vp.Constants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

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
