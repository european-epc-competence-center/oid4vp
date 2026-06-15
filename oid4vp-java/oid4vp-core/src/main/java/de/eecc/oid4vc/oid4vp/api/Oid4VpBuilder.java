package de.eecc.oid4vc.oid4vp.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import de.eecc.oid4vc.oid4vp.store.CaffeinePresentationRequestRepository;
import de.eecc.oid4vc.oid4vp.store.PresentationRequestRepository;
import de.eecc.oid4vc.oid4vp.verifier.HttpPresentationVerifier;
import de.eecc.oid4vc.oid4vp.verifier.PresentationVerifier;

import java.security.SecureRandom;

/**
 * Builds a configured {@link Oid4Vp} instance with optional dependency overrides.
 */
public final class Oid4VpBuilder {

    private Oid4VpOptions options;
    private PresentationRequestRepository repository;
    private PresentationVerifier verifier;
    private ObjectMapper objectMapper;
    private SecureRandom secureRandom;

    Oid4VpBuilder() {}

    public Oid4VpBuilder options(Oid4VpOptions options) {
        this.options = options;
        return this;
    }

    public Oid4VpBuilder repository(PresentationRequestRepository repository) {
        this.repository = repository;
        return this;
    }

    public Oid4VpBuilder verifier(PresentationVerifier verifier) {
        this.verifier = verifier;
        return this;
    }

    public Oid4VpBuilder objectMapper(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
        return this;
    }

    public Oid4VpBuilder secureRandom(SecureRandom secureRandom) {
        this.secureRandom = secureRandom;
        return this;
    }

    public Oid4Vp build() {
        if (options == null) {
            throw new IllegalStateException("Oid4VpOptions is required");
        }
        ObjectMapper mapper = objectMapper != null ? objectMapper : new ObjectMapper();
        PresentationRequestRepository repo = repository != null
                ? repository
                : new CaffeinePresentationRequestRepository(options.requestTtl());
        PresentationVerifier presentationVerifier = verifier != null
                ? verifier
                : new HttpPresentationVerifier(options.verifierUrl(), mapper);
        SecureRandom random = secureRandom != null ? secureRandom : new SecureRandom();
        return new Oid4Vp(options, repo, presentationVerifier, mapper, random);
    }
}
