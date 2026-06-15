package de.eecc.oid4vc.oid4vp.api;

import de.eecc.oid4vc.oid4vp.request.PresentationRequest;
import de.eecc.oid4vc.oid4vp.request.PresentationRequestDefinition;
import lombok.Builder;

import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Inputs for creating a new OID4VP authorization request.
 *
 * <p>Set application-specific fields via {@link #builderSupplier()} (and optionally {@link #beforeSave()})
 * <strong>before</strong> persistence — do not mutate the request after {@link Oid4Vp#generatePresentationRequest}
 * returns.
 *
 * <pre>{@code
 * oid4Vp.generatePresentationRequest(
 *     GenerateRequestOptions.<ExtendedPresentationRequest>builder(Gs1LicenseRequest.INSTANCE)
 *         .redirect(false)
 *         .builderSupplier(() -> ExtendedPresentationRequest.builder()
 *             .purpose(Oid4VpPurpose.ADD_ORG_GCPS)
 *             .organizationId(orgId))
 *         .build());
 * }</pre>
 *
 * @param <T> request type; use a subclass of {@link PresentationRequest} for application-specific fields
 */
@Builder
public record GenerateRequestOptions<T extends PresentationRequest>(
        PresentationRequestDefinition definition,
        Boolean redirect,
        Supplier<? extends PresentationRequest.PresentationRequestBuilder<?, ?>> builderSupplier,
        Consumer<T> beforeSave
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
        return new GenerateRequestOptions<>(definition, true, PresentationRequest::builder, null);
    }

    public static <T extends PresentationRequest> GenerateRequestOptionsBuilder<T> builder(
            PresentationRequestDefinition definition) {
        return new GenerateRequestOptionsBuilder<T>().definition(definition);
    }
}
