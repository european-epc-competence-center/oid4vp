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

After verification, extract claims via the same definition used to generate the request:

```java
import de.eecc.oid4vc.oid4vp.PresentationClaims;
import de.eecc.oid4vc.oid4vp.request.PresentationRequest;
import de.eecc.oid4vc.oid4vp.request.template.gs1.Gs1LicenseRequest;

PresentationRequest request = oid4Vp.generatePresentationRequest(Gs1LicenseRequest.INSTANCE);
// ... after direct_post verification ...
PresentationClaims claims = oid4Vp.extractPresentationClaims(Gs1LicenseRequest.INSTANCE, request);
List<String> gcps = claims.values();
```

### OAuth 2.0 login completion (`response_code`)

After the wallet delivers a verified presentation via `direct_post`, a one-time `response_code` bridges the
OpenID4VP step to your OAuth 2.0 token endpoint.

**Flow**

1. The wallet POSTs `vp_token` and `state` to your `response_uri`.
2. You call `oid4Vp.processDirectPost(vpToken, state, handler)`; the library verifies presentations and invokes your handler.
3. For login flows, return `DirectPostResult.issueResponseCode()` from the handler. The library stores the verified session and issues a single-use `response_code`.
4. The frontend obtains that code in one of two ways:
   - **Wallet redirect** (`redirect(true)`): the wallet redirects the user agent to `redirect_uri#response_code=…`.
   - **QR / poll** (`redirect(false)`): the frontend polls `GET …/response/{state}` until it receives `{ "response_code": "…" }` (204 while pending).
5. The frontend exchanges `state` + `response_code` at your OAuth 2.0 token endpoint (for example `POST …/token`).
6. You resolve the stored presentation by `response_code`, validate it matches `state`, extract claims, issue access/refresh tokens, and call `oid4Vp.invalidateResponseCode(request)` so the code cannot be reused.

Non-login flows (for example “add credential without signing in”) can return `DirectPostResult.complete()` instead; polling then returns `{ "completed": true }` with no `response_code`.

**Server-side example** (adapt paths and token issuance to your application):

```java
import com.fasterxml.jackson.databind.JsonNode;
import de.eecc.oid4vc.oid4vp.PresentationClaims;
import de.eecc.oid4vc.oid4vp.VpTokenResponse;
import de.eecc.oid4vc.oid4vp.api.DirectPostResult;

import java.util.Optional;

// Wallet direct_post → POST /api/auth/oid4vp/response
VpTokenResponse.DirectPostResponse directPostResponse =
        oid4Vp.processDirectPost(vpToken, state, (LoginPresentationRequest request, JsonNode vpTokenNode) -> {
            PresentationClaims claims = oid4Vp.extractPresentationClaims(myDefinition, vpTokenNode);
            // persist claims on request if needed for token redemption
            return DirectPostResult.issueResponseCode();
        });

// QR poll → GET /api/auth/oid4vp/response/{state}  (204 until ready)
Optional<VpTokenResponse.PollResponse> poll = oid4Vp.pollPresentationStatus(state);

// Token endpoint → POST /api/auth/oid4vp/token  (state + response_code)
LoginPresentationRequest stored = oid4Vp
        .findPresentationRequestByResponseCode(responseCode, LoginPresentationRequest.class)
        .orElseThrow(() -> new BadRequestException("Unknown or expired response_code"));
if (!stored.getState().equals(state)) {
    throw new BadRequestException("response_code does not match state");
}
PresentationClaims claims = oid4Vp.extractPresentationClaims(myDefinition, stored);
TokenResult tokens = issueSessionTokens(claims); // your JWT / OAuth2 issuance
oid4Vp.invalidateResponseCode(stored);
```


Lower-level access: `definition.extractPresentationClaims(vpTokenNode)` or `PresentationParser` for format-specific parsing.

#### Built-in template: GS1 License Presentation

The library ships a ready-made definition for GS1 Company Prefix and Prefix License credentials (`Gs1LicenseRequest.INSTANCE`).

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
├── src/main/java/de/eecc/oid4vc/oid4vp/
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
└── src/test/java/de/eecc/oid4vc/oid4vp/
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
