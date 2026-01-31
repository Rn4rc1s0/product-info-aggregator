package com.kramp.productinfo.domain.model;

import java.util.Objects;

public record AggregatedProduct(
        ProductDetails product,
        PricingInfo pricing,
        AvailabilityInfo availability,
        CustomerContext customer
) {
    public AggregatedProduct {
        Objects.requireNonNull(product, "product");
        pricing = (pricing == null) ? PricingInfo.unavailable("NOT_AVAILABLE") : pricing;
        availability = (availability == null) ? AvailabilityInfo.unknown() : availability;
        customer = (customer == null) ? CustomerContext.standard() : customer;
    }
}
