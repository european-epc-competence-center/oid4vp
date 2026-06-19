package de.eecc.oid4vc.oid4vp;

import de.eecc.oid4vc.oid4vp.request.PresentationRequestDefinition;
import de.eecc.oid4vc.oid4vp.request.template.gs1.Gs1LicenseRequest;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class DcqlQueryTest {

    @Test
    void credentialQuery_typeValues_fromMeta() {
        var query = new DcqlQuery.CredentialQuery(
                "c1",
                Constants.VP_FORMAT_JWT_VC_JSON,
                Map.of("type_values", List.of(
                        List.of("VerifiableCredential", "MyType"))),
                List.of(),
                true);

        assertThat(query.typeValues()).containsExactly(List.of("VerifiableCredential", "MyType"));
    }

    @Test
    void credentialQuery_subjectIds_fromIdClaimValues() {
        var query = new DcqlQuery.CredentialQuery(
                "c1",
                Constants.VP_FORMAT_JWT_VC_JSON,
                Map.of(),
                List.of(new DcqlQuery.ClaimsQuery(null, List.of("id"), List.of("did:example:holder"))),
                true);

        assertThat(query.subjectIds()).containsExactly("did:example:holder");
    }

    @Test
    void credentialQuery_claimValueConstraints_andLookupByClaimId() {
        var query = new DcqlQuery.CredentialQuery(
                "c1",
                Constants.VP_FORMAT_JWT_VC_JSON,
                Map.of(),
                List.of(
                        new DcqlQuery.ClaimsQuery("licenseValue", List.of("licenseValue"), List.of("0614141")),
                        new DcqlQuery.ClaimsQuery("partyGLN", List.of("organization", "gs1:partyGLN"), List.of())),
                true);

        assertThat(query.claimValueConstraints())
                .containsEntry("licenseValue", List.of("0614141"));

        assertThat(query.valuesForClaimId("licenseValue")).contains(List.of("0614141"));
        assertThat(query.pathForClaimId("licenseValue"))
                .contains(List.of("credentialSubject", "licenseValue"));
        assertThat(query.pathForClaimId("partyGLN"))
                .contains(List.of("credentialSubject", "organization", "gs1:partyGLN"));
    }

    @Test
    void claimsQuery_normalizesBarePathsUnderCredentialSubject() {
        var claim = DcqlQuery.subjectClaim("givenName", List.of("given_name"));

        assertThat(claim.pathSegments()).containsExactly("credentialSubject", "given_name");
        assertThat(claim.isUnderCredentialSubject()).isTrue();

        var issuerClaim = new DcqlQuery.ClaimsQuery("issuer", List.of("issuer"), List.of("did:example:issuer"));
        assertThat(issuerClaim.pathSegments()).containsExactly("issuer");
        assertThat(issuerClaim.isUnderCredentialSubject()).isFalse();

        var subjectIdClaim = new DcqlQuery.ClaimsQuery("id", List.of("id"), List.of("did:example:holder"));
        assertThat(subjectIdClaim.pathSegments()).containsExactly("credentialSubject", "id");
        assertThat(subjectIdClaim.isSubjectIdPath()).isTrue();
    }

    @Test
    void credentialQuery_arbitraryPathClaims_inMatchCriteria() {
        var query = new DcqlQuery.CredentialQuery(
                "student_card",
                Constants.VP_FORMAT_JWT_VC_JSON,
                Map.of("type_values", List.of(List.of("VerifiableCredential", "StudentCard"))),
                List.of(
                        DcqlQuery.subjectClaim(null, List.of("given_name")),
                        DcqlQuery.subjectClaim(
                                "enrollmentYear",
                                List.of("enrollmentYear"),
                                List.of("2024", "2025")),
                        DcqlQuery.subjectClaim(null, List.of("address", "postal_code"), List.of("90210", "90211")),
                        DcqlQuery.subjectClaim(
                                "partyGLN",
                                List.of("organization", "gs1:partyGLN"),
                                List.of("9501100000000"))),
                true);

        DcqlQuery.CredentialMatchCriteria criteria = query.toMatchCriteria();

        assertThat(criteria.credentialQueryId()).isEqualTo("student_card");
        assertThat(criteria.claimValueConstraints())
                .containsExactlyEntriesOf(Map.of(
                        "enrollmentYear", List.of("2024", "2025"),
                        "postal_code", List.of("90210", "90211"),
                        "partyGLN", List.of("9501100000000")));

        assertThat(query.pathForClaimId("given_name"))
                .contains(List.of("credentialSubject", "given_name"));
        assertThat(query.pathForClaimId("enrollmentYear"))
                .contains(List.of("credentialSubject", "enrollmentYear"));
        assertThat(query.pathForClaimId("postal_code"))
                .contains(List.of("credentialSubject", "address", "postal_code"));
        assertThat(query.findClaim("partyGLN"))
                .isPresent()
                .get()
                .extracting(DcqlQuery.ClaimsQuery::pathSegments)
                .isEqualTo(List.of("credentialSubject", "organization", "gs1:partyGLN"));

        DcqlQuery.Query dcqlQuery = new DcqlQuery.Query(List.of(query));
        assertThat(dcqlQuery.matchCriteria()).containsExactly(criteria);
    }

    @Test
    void credentialQuery_toMatchCriteria() {
        var query = new DcqlQuery.CredentialQuery(
                "gs1_license",
                Constants.VP_FORMAT_JWT_VC_JSON,
                Map.of("type_values", List.of(
                        List.of("VerifiableCredential", "GS1PrefixLicenseCredential"))),
                List.of(new DcqlQuery.ClaimsQuery("id", List.of("id"), List.of("did:web:holder.example"))),
                false);

        DcqlQuery.CredentialMatchCriteria criteria = query.toMatchCriteria();

        assertThat(criteria.credentialQueryId()).isEqualTo("gs1_license");
        assertThat(criteria.format()).isEqualTo(Constants.VP_FORMAT_JWT_VC_JSON);
        assertThat(criteria.typeValues()).hasSize(1);
        assertThat(criteria.subjectIds()).containsExactly("did:web:holder.example");
        assertThat(criteria.issuers()).isEmpty();
        assertThat(criteria.requireCryptographicHolderBinding()).isFalse();
    }

    @Test
    void credentialQuery_issuers_fromIssuerClaimValues() {
        var query = new DcqlQuery.CredentialQuery(
                "c1",
                Constants.VP_FORMAT_JWT_VC_JSON,
                Map.of(),
                List.of(new DcqlQuery.ClaimsQuery("issuer", List.of("issuer"), List.of("did:example:issuer"))),
                true);

        assertThat(query.issuers()).containsExactly("did:example:issuer");
    }

    @Test
    void withIdentityConstraints_mergesIssuerAndSubjectIdIntoAllCredentials() {
        DcqlQuery.Query base = Gs1LicenseRequest.INSTANCE.templateDcqlQuery();
        DcqlQuery.Query constrained = DcqlQuery.withIdentityConstraints(
                base,
                List.of("did:example:issuer"),
                List.of("did:example:holder"));

        assertThat(constrained.credentials()).hasSize(2);
        for (DcqlQuery.CredentialQuery credential : constrained.credentials()) {
            assertThat(credential.subjectIds()).containsExactly("did:example:holder");
            assertThat(credential.issuers()).containsExactly("did:example:issuer");
            assertThat(credential.claims())
                    .anyMatch(claim -> claim.isSubjectIdPath() && claim.values().contains("did:example:holder"))
                    .anyMatch(claim -> claim.isIssuerPath() && claim.values().contains("did:example:issuer"));
        }
    }

    @Test
    void withIdentityConstraints_supportsMultipleIssuersAndSubjectIds() {
        DcqlQuery.Query base = Gs1LicenseRequest.INSTANCE.templateDcqlQuery();
        DcqlQuery.Query constrained = DcqlQuery.withIdentityConstraints(
                base,
                List.of("did:example:issuer-a", "did:example:issuer-b"),
                List.of("did:example:holder-1", "did:example:holder-2"));

        for (DcqlQuery.CredentialQuery credential : constrained.credentials()) {
            assertThat(credential.issuers())
                    .containsExactly("did:example:issuer-a", "did:example:issuer-b");
            assertThat(credential.subjectIds())
                    .containsExactly("did:example:holder-1", "did:example:holder-2");
            assertThat(credential.findClaim("issuer"))
                    .isPresent()
                    .get()
                    .extracting(DcqlQuery.ClaimsQuery::values)
                    .isEqualTo(List.of("did:example:issuer-a", "did:example:issuer-b"));
            assertThat(credential.findClaim("id"))
                    .isPresent()
                    .get()
                    .extracting(DcqlQuery.ClaimsQuery::values)
                    .isEqualTo(List.of("did:example:holder-1", "did:example:holder-2"));
        }
    }

    @Test
    void presentationRequestDefinition_requiresIssuersAndSubjectIds_chainsConstraints() {
        PresentationRequestDefinition definition = Gs1LicenseRequest.INSTANCE
                .requiresIssuers(List.of("did:example:issuer-a", "did:example:issuer-b"))
                .requiresSubjectIds(List.of("did:example:holder-1", "did:example:holder-2"));

        assertThat(definition.requiredIssuers())
                .containsExactly("did:example:issuer-a", "did:example:issuer-b");
        assertThat(definition.requiredSubjectIds())
                .containsExactly("did:example:holder-1", "did:example:holder-2");
        assertThat(definition.dcqlQuery().credentials().getFirst().issuers())
                .containsExactly("did:example:issuer-a", "did:example:issuer-b");
        assertThat(definition.dcqlQuery().credentials().getFirst().subjectIds())
                .containsExactly("did:example:holder-1", "did:example:holder-2");
    }

    @Test
    void presentationRequestDefinition_requiresIssuerAndSubjectId_chainsConstraints() {
        PresentationRequestDefinition definition = Gs1LicenseRequest.INSTANCE
                .requiresIssuer("did:example:issuer")
                .requiresSubjectId("did:example:holder");

        assertThat(definition.requiredIssuers()).containsExactly("did:example:issuer");
        assertThat(definition.requiredSubjectIds()).containsExactly("did:example:holder");
        assertThat(definition.dcqlQuery().credentials().getFirst().issuers())
                .containsExactly("did:example:issuer");
        assertThat(definition.dcqlQuery().credentials().getFirst().subjectIds())
                .containsExactly("did:example:holder");
    }

    @Test
    void presentationRequestDefinition_requiresIssuer_onlyAddsIssuerConstraint() {
        PresentationRequestDefinition definition =
                Gs1LicenseRequest.INSTANCE.requiresIssuer("did:example:issuer");

        assertThat(definition.requiredIssuers()).containsExactly("did:example:issuer");
        assertThat(definition.requiredSubjectIds()).isEmpty();
    }

    @Test
    void query_credentialsForFormat_and_matchCriteria() {
        DcqlQuery.Query query = Gs1LicenseRequest.INSTANCE.dcqlQuery();

        assertThat(query.credentialsForFormat(Constants.VP_FORMAT_LDP_VC)).hasSize(1);
        assertThat(query.firstCredentialForFormat(Constants.VP_FORMAT_JWT_VC_JSON)).isPresent();
        assertThat(query.matchCriteria()).hasSize(2);
        assertThat(query.credentialById(Gs1LicenseRequest.CREDENTIAL_ID)).isPresent();
    }

    @Test
    void gs1Template_claimIds_and_matchCriteriaHelpers() {
        assertThat(Gs1LicenseRequest.INSTANCE.templateClaimIds())
                .containsExactly(
                        Gs1LicenseRequest.CLAIM_LICENSE_VALUE,
                        Gs1LicenseRequest.CLAIM_ORGANIZATION_NAME,
                        Gs1LicenseRequest.CLAIM_PARTY_GLN);

        assertThat(Gs1LicenseRequest.INSTANCE.matchCriteriaForFormat(Constants.VP_FORMAT_JWT_VC_JSON))
                .isPresent()
                .get()
                .extracting(DcqlQuery.CredentialMatchCriteria::credentialQueryId)
                .isEqualTo(Gs1LicenseRequest.CREDENTIAL_ID);
    }
}
