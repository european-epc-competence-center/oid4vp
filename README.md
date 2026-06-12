# oid4vp Java Library

Java library for generating and processing [OpenID4VP](https://openid.net/specs/openid-4-verifiable-presentations-1_0.html) presentation requests.

## Requirements

- Java 25 JDK
- Maven 3.9+

## Installation

```xml
<dependency>
  <groupId>de.eecc.oid4vc</groupId>
  <artifactId>oid4vp</artifactId>
  <version>0.1.0-SNAPSHOT</version>
</dependency>
```

## Quick Start

`verifierUrl` must point to a compatible credential verification API. Any verifier that exposes the
expected REST endpoints will work; we recommend the
[EECC VC Verifier](https://github.com/european-epc-competence-center/vc-verifier), which provides
presentation and credential verification (including GS1-specific rules).

```java
import de.eecc.oid4vc.oid4vp.*;
import de.eecc.oid4vc.oid4vp.api.*;
import de.eecc.oid4vc.oid4vp.request.*;
import de.eecc.oid4vc.oid4vp.request.template.gs1.Gs1LicenseRequest;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

Oid4Vp oid4Vp = Oid4Vp.create(Oid4VpOptions.builder()
        .verifierUrl("http://vc-verifier:3000/api/verifier") // recommended: EECC VC Verifier (see link above)
        .responseUri("https://example.com/api/auth/oid4vp/response")
        .redirectUri("https://example.com/login/oid4vp")
        .requestUriBaseUrl("https://example.com/api/auth/oid4vp")
        .requestUriEnabled(true)
        .build());

@Getter
@Setter
@SuperBuilder
class LoginPresentationRequest extends PresentationRequest {
    // add custom properties for internal use, e.g. to distinguish different purposes of presentations
    private String purpose;
}

LoginPresentationRequest request = oid4Vp.generatePresentationRequest(
        GenerateRequestOptions.<LoginPresentationRequest>builder(myPresentationDefinition)
                .redirect(true)
                .builderSupplier(() -> LoginPresentationRequest.builder().purpose("LOGIN"))
                .build());

String walletUrl = oid4Vp.toOpenId4VpUrl(request);
```

### Presentation Request Definitions

A presentation request definition describes **what** the verifier asks for: a [DCQL](https://openid.net/specs/openid-4-verifiable-presentations-1_0.html) query (credential types, formats, claim paths) and optional `client_metadata`. Transport fields such as nonce, state, and response URI are added by `Oid4Vp`.

Implement `PresentationRequestDefinition` and pass it to `generatePresentationRequest`:

```java
import de.eecc.oid4vc.oid4vp.Constants;
import de.eecc.oid4vc.oid4vp.DcqlQuery;
import de.eecc.oid4vc.oid4vp.request.PresentationRequestDefinition;

PresentationRequestDefinition myDefinition = new PresentationRequestDefinition() {
    @Override
    public DcqlQuery.Query dcqlQuery() {
        return new DcqlQuery.Query(List.of(
                new DcqlQuery.CredentialQuery(
                        "my_credential",
                        Constants.VP_FORMAT_JWT_VC_JSON,
                        Map.of("type_values", List.of(List.of("VerifiableCredential", "MyCredentialType"))),
                        List.of(new DcqlQuery.ClaimsQuery("email", List.of("email"))),
                        true)));
    }
};

PresentationRequest request = oid4Vp.generatePresentationRequest(myDefinition);
```

After verification, add your own logic to read claims from the `vp_token` (see `PresentationParser` or the GS1 template below).

#### Built-in template: GS1 License Presentation

The library also ships a ready-made definition for GS1 Company Prefix and Prefix License credentials:

```java
import de.eecc.oid4vc.oid4vp.PresentationClaims;
import de.eecc.oid4vc.oid4vp.request.PresentationRequest;
import de.eecc.oid4vc.oid4vp.request.template.gs1.Gs1LicenseRequest;

PresentationRequest request = oid4Vp.generatePresentationRequest(Gs1LicenseRequest.INSTANCE);
String walletUrl = oid4Vp.toOpenId4VpUrl(request);

// After direct_post verification:
PresentationClaims claims = Gs1LicenseRequest.extractPresentationClaims(vpTokenNode);
List<String> gcps = claims.values();
```

Application-specific attributes (for example `purpose` or an organisation id) belong on a
subclass of `PresentationRequest` and are never serialized to the wallet.

## Development

```bash
cd oid4vp-java
mvn test
mvn package
```

### Project Structure

```
oid4vp-java/
├── src/main/java/de/eecc/oid4vp/
│   ├── api/                 # Public API: Oid4Vp, *Options, DirectPostHandler
│   ├── request/             # PresentationRequest, PublicPresentationRequest, PresentationRequestDefinition
│   │   └── template/        # Built-in presentation request templates (e.g. gs1)
│   ├── store/               # PresentationRequestStore
│   ├── verifier/            # VerifierClient
│   ├── vp/                  # PresentationParser
│   ├── exception/           # Oid4VpException
│   ├── Constants.java       # Protocol constants
│   ├── DcqlQuery.java       # DCQL models
│   └── ...
└── src/test/java/de/eecc/oid4vp/
```

## Repository Overview

```
/
├── oid4vp-java/             # Java library (Maven, Java 25)
├── README.md
└── LICENSE
```

## License

Apache License 2.0 — see [LICENSE](LICENSE).
