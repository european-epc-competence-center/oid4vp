package de.eecc.oid4vc.oid4vp.request.template.gs1;

import com.fasterxml.jackson.databind.JsonNode;
import de.eecc.oid4vc.oid4vp.Constants;
import de.eecc.oid4vc.oid4vp.DcqlQuery;
import de.eecc.oid4vc.oid4vp.PresentationClaims;
import de.eecc.oid4vc.oid4vp.request.PresentationRequestDefinition;
import de.eecc.oid4vc.oid4vp.vp.PresentationParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

/**
 * OID4VP presentation request for GS1 Company Prefix or Prefix License credentials.
 *
 * <p>Pass {@link #INSTANCE} to {@link de.eecc.oid4vc.oid4vp.api.Oid4Vp#generatePresentationRequest}
 * to request {@code GS1CompanyPrefixLicenseCredential} or {@code GS1PrefixLicenseCredential}
 * with {@code licenseValue}, {@code organization.gs1:organizationName}, and
 * {@code organization.gs1:partyGLN}.
 */
public final class Gs1LicenseRequest implements PresentationRequestDefinition {

    private static final Logger log = LoggerFactory.getLogger(Gs1LicenseRequest.class);

    public static final Gs1LicenseRequest INSTANCE = new Gs1LicenseRequest();

    public static final String CREDENTIAL_ID = "gs1_license";

    public static final String CLAIM_LICENSE_VALUE = "licenseValue";
    /** JSON-LD expanded GS1 vocabulary alias for {@link #CLAIM_LICENSE_VALUE}. */
    public static final String GS1_LICENSE_VALUE = "gs1:licenseValue";
    public static final String CLAIM_ORGANIZATION_NAME = "organizationName";
    public static final String CLAIM_PARTY_GLN = "partyGLN";

    public static final String TYPE_COMPANY_PREFIX = "GS1CompanyPrefixLicenseCredential";
    public static final String TYPE_PREFIX = "GS1PrefixLicenseCredential";

    public static final String SUBJECT_ORGANIZATION = "organization";
    public static final String GS1_ORGANIZATION_NAME = "gs1:organizationName";
    public static final String GS1_PARTY_GLN = "gs1:partyGLN";

    /** Fallback when {@link #CLAIM_LICENSE_VALUE} is absent on the credential subject. */
    public static final String ALTERNATIVE_LICENSE_VALUE = "alternativeLicenseValue";

    public static final List<Object> PATH_LICENSE_VALUE = List.of(CLAIM_LICENSE_VALUE);
    public static final List<Object> PATH_ORGANIZATION_NAME =
            List.of(SUBJECT_ORGANIZATION, GS1_ORGANIZATION_NAME);
    public static final List<Object> PATH_PARTY_GLN = List.of(SUBJECT_ORGANIZATION, GS1_PARTY_GLN);

    private static final List<List<String>> TYPE_VALUES = List.of(
            List.of("VerifiableCredential", TYPE_COMPANY_PREFIX),
            List.of("VerifiableCredential", TYPE_PREFIX)
    );

    /**
     * GS1 license claims mapped to {@link PresentationClaims}.
     */
    public record Gs1PresentationClaims(
            String partyGln,
            String organizationName,
            List<String> gcps,
            String credentialType
    ) implements PresentationClaims {

        public Gs1PresentationClaims(String partyGln, String organizationName, List<String> gcps) {
            this(partyGln, organizationName, gcps, null);
        }

        @Override
        public String identifier() {
            return partyGln;
        }

        @Override
        public String name() {
            return organizationName;
        }

        @Override
        public List<String> values() {
            return gcps;
        }

        @Override
        public String credentialType() {
            return credentialType;
        }

        @Override
        public Map<String, Object> claimValues() {
            Map<String, Object> claims = new LinkedHashMap<>();
            claims.put(CLAIM_LICENSE_VALUE, List.copyOf(gcps));
            if (organizationName != null) {
                claims.put(CLAIM_ORGANIZATION_NAME, organizationName);
            }
            if (partyGln != null) {
                claims.put(CLAIM_PARTY_GLN, partyGln);
            }
            if (credentialType != null) {
                claims.put("credentialType", credentialType);
            }
            return Map.copyOf(claims);
        }
    }

    private Gs1LicenseRequest() {}

    @Override
    public DcqlQuery.Query dcqlQuery() {
        List<DcqlQuery.ClaimsQuery> claims = List.of(
                new DcqlQuery.ClaimsQuery(CLAIM_LICENSE_VALUE, PATH_LICENSE_VALUE),
                new DcqlQuery.ClaimsQuery(CLAIM_ORGANIZATION_NAME, PATH_ORGANIZATION_NAME),
                new DcqlQuery.ClaimsQuery(CLAIM_PARTY_GLN, PATH_PARTY_GLN)
        );

        DcqlQuery.CredentialQuery ldpVcQuery = new DcqlQuery.CredentialQuery(
                CREDENTIAL_ID,
                Constants.VP_FORMAT_LDP_VC,
                Map.of("type_values", TYPE_VALUES),
                claims,
                true);

        DcqlQuery.CredentialQuery jwtVcQuery = new DcqlQuery.CredentialQuery(
                CREDENTIAL_ID,
                Constants.VP_FORMAT_JWT_VC_JSON,
                Map.of("type_values", TYPE_VALUES),
                claims,
                true);

        return new DcqlQuery.Query(List.of(ldpVcQuery, jwtVcQuery));
    }

    /**
     * Extracts GCP and organisation metadata from a verified {@code vp_token}.
     * Organisation fields use the first GCP credential encountered.
     */
    @Override
    public PresentationClaims extractPresentationClaims(JsonNode vpTokenNode) {
        List<String> extractedGcps = new ArrayList<>();
        String firstOrganizationName = null;
        String firstPartyGln = null;
        String firstCredentialType = null;
        LinkedHashSet<String> seenGcps = new LinkedHashSet<>();

        for (Map.Entry<String, JsonNode> entry : vpTokenNode.properties()) {
            JsonNode presentations = entry.getValue();
            if (!presentations.isArray()) {
                continue;
            }

            for (JsonNode presentationNode : presentations) {
                List<String> presentationGcps = extractLicenseValues(presentationNode);
                for (String gcp : presentationGcps) {
                    String normalized = gcp == null ? "" : gcp.trim();
                    if (normalized.isBlank()) {
                        continue;
                    }
                    if (seenGcps.add(normalized)) {
                        extractedGcps.add(normalized);
                    }

                    if (firstOrganizationName == null) {
                        firstOrganizationName = extractOrganizationName(presentationNode);
                        firstPartyGln = extractPartyGln(presentationNode);
                        firstCredentialType = extractCredentialType(presentationNode);
                    }
                }
            }
        }

        return new Gs1PresentationClaims(
                firstPartyGln, firstOrganizationName, extractedGcps, firstCredentialType);
    }

    /**
     * Returns the GS1 license credential type from the first matching credential in a presentation.
     */
    public static String extractCredentialType(JsonNode presentationNode) {
        JsonNode root = PresentationParser.presentationRoot(presentationNode);
        if (root == null || root.isMissingNode()) {
            return null;
        }

        String type = credentialTypeFromVerifiableCredentials(root.get("verifiableCredential"));
        if (type != null) {
            return type;
        }

        JsonNode vp = root.get("vp");
        if (vp != null && vp.isObject()) {
            return credentialTypeFromVerifiableCredentials(vp.get("verifiableCredential"));
        }

        return null;
    }

    /**
     * Extracts all {@code licenseValue} entries from a GS1 credential presentation.
     */
    public static List<String> extractLicenseValues(JsonNode presentationNode) {
        List<JsonNode> subjects = findCredentialSubjectsWithLicense(presentationNode);
        if (subjects.isEmpty()) {
            log.warn("No credentialSubject found in presentation");
            return List.of();
        }

        LinkedHashSet<String> values = new LinkedHashSet<>();
        for (JsonNode subject : subjects) {
            String license = licenseValueFromSubject(subject);
            if (license != null) {
                values.add(license);
            }
        }

        if (values.isEmpty()) {
            log.warn("No licenseValue or alternativeLicenseValue found in presentation credentials");
        }
        return new ArrayList<>(values);
    }

    private static String credentialTypeFromVerifiableCredentials(JsonNode verifiableCredential) {
        if (verifiableCredential == null || !verifiableCredential.isArray()) {
            return null;
        }

        for (JsonNode credentialEntry : verifiableCredential) {
            String type = credentialTypeFromCredentialEntry(credentialEntry);
            if (type != null) {
                return type;
            }
        }

        return null;
    }

    private static String credentialTypeFromCredentialEntry(JsonNode credentialEntry) {
        if (credentialEntry == null || credentialEntry.isMissingNode()) {
            return null;
        }

        if (credentialEntry.isTextual()) {
            String compactJwt = PresentationParser.compactJwtFromTextualCredential(credentialEntry.asText());
            if (compactJwt == null) {
                return null;
            }
            return credentialTypeFromJwtPayload(PresentationParser.parseJwtPayload(compactJwt));
        }

        if (!credentialEntry.isObject()) {
            return null;
        }

        String type = credentialTypeFromTypeArray(credentialEntry.get("type"));
        if (type != null) {
            return type;
        }

        String compactJwt = PresentationParser.extractEnvelopedJwtFromCredential(credentialEntry);
        if (compactJwt == null) {
            return null;
        }

        return credentialTypeFromJwtPayload(PresentationParser.parseJwtPayload(compactJwt));
    }

    private static String credentialTypeFromJwtPayload(JsonNode payload) {
        if (payload == null || payload.isMissingNode()) {
            return null;
        }

        String type = credentialTypeFromTypeArray(payload.get("type"));
        if (type != null) {
            return type;
        }

        JsonNode nestedVc = payload.get("vc");
        if (nestedVc != null && nestedVc.isObject()) {
            type = credentialTypeFromTypeArray(nestedVc.get("type"));
            if (type != null) {
                return type;
            }
        }

        JsonNode nestedCredential = payload.get("credential");
        if (nestedCredential != null && nestedCredential.isObject()) {
            return credentialTypeFromTypeArray(nestedCredential.get("type"));
        }

        return null;
    }

    private static String credentialTypeFromTypeArray(JsonNode typeNode) {
        if (typeNode == null || !typeNode.isArray()) {
            return null;
        }

        for (JsonNode entry : typeNode) {
            if (!entry.isTextual()) {
                continue;
            }
            String value = entry.asText();
            if (TYPE_COMPANY_PREFIX.equals(value) || TYPE_PREFIX.equals(value)) {
                return value;
            }
        }

        return null;
    }

    private static String licenseValueFromSubject(JsonNode credentialSubject) {
        if (credentialSubject == null || credentialSubject.isMissingNode()) {
            return null;
        }

        String license = textClaim(credentialSubject, CLAIM_LICENSE_VALUE);
        if (license != null) {
            return license;
        }

        license = textClaim(credentialSubject, GS1_LICENSE_VALUE);
        if (license != null) {
            return license;
        }

        return textClaim(credentialSubject, ALTERNATIVE_LICENSE_VALUE);
    }

    private static String textClaim(JsonNode node, String field) {
        JsonNode value = node.get(field);
        if (value == null || value.isNull() || !value.isTextual()) {
            return null;
        }
        String text = value.asText().trim();
        return text.isBlank() ? null : text;
    }

    private static String extractOrganizationName(JsonNode presentationNode) {
        JsonNode credentialSubject = PresentationParser.findCredentialSubject(presentationNode);
        if (credentialSubject == null || credentialSubject.isMissingNode()) {
            return null;
        }

        JsonNode organization = credentialSubject.get(SUBJECT_ORGANIZATION);
        if (organization == null || organization.isMissingNode()) {
            return null;
        }

        JsonNode nameNode = organization.get(GS1_ORGANIZATION_NAME);
        if (nameNode == null || nameNode.isNull() || !nameNode.isTextual()) {
            return null;
        }

        String value = nameNode.asText().trim();
        return value.isBlank() ? null : value;
    }

    private static String extractPartyGln(JsonNode presentationNode) {
        JsonNode credentialSubject = PresentationParser.findCredentialSubject(presentationNode);
        if (credentialSubject == null || credentialSubject.isMissingNode()) {
            return null;
        }

        JsonNode organization = credentialSubject.get(SUBJECT_ORGANIZATION);
        if (organization == null || organization.isMissingNode()) {
            return null;
        }

        JsonNode glnNode = organization.get(GS1_PARTY_GLN);
        if (glnNode == null || glnNode.isNull() || !glnNode.isTextual()) {
            return null;
        }

        String value = glnNode.asText().trim();
        return value.isBlank() ? null : value;
    }

    private static List<JsonNode> findCredentialSubjectsWithLicense(JsonNode presentationNode) {
        return PresentationParser.collectCredentialSubjects(presentationNode).stream()
                .filter(Gs1LicenseRequest::hasLicenseClaim)
                .toList();
    }

    private static boolean hasLicenseClaim(JsonNode subject) {
        return textClaim(subject, CLAIM_LICENSE_VALUE) != null
                || textClaim(subject, GS1_LICENSE_VALUE) != null
                || textClaim(subject, ALTERNATIVE_LICENSE_VALUE) != null;
    }
}
