package de.eecc.oid4vc.oid4vp.api;

import de.eecc.oid4vc.oid4vp.request.PresentationRequest;
import de.eecc.oid4vc.oid4vp.request.PresentationRequestDefinition;
import lombok.Builder;

import java.util.function.Supplier;

/**
 * Inputs for creating a new OID4VP authorization request.
 *
 * @param <T> request type; use a subclass of {@link PresentationRequest} for application-specific fields
 */
@Builder
public record GenerateRequestOptions<T extends PresentationRequest>(
        PresentationRequestDefinition definition,
        Boolean redirect,
        Supplier<? extends PresentationRequest.PresentationRequestBuilder<?, ?>> builderSupplier
) {

    public GenerateRequestOptions {
        if (redirect == null) {
            redirect = true;
        }
        if (builderSupplier == null) {
            builderSupplier = PresentationRequest::builder;
        }
    }

    public static GenerateRequestOptions<PresentationRequest> of(PresentationRequestDefinition definition) {
        return new GenerateRequestOptions<>(definition, true, PresentationRequest::builder);
    }

    public static <T extends PresentationRequest> GenerateRequestOptionsBuilder<T> builder(
            PresentationRequestDefinition definition) {
        return new GenerateRequestOptionsBuilder<T>().definition(definition);
    }
}
