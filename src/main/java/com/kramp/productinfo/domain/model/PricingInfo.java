package com.kramp.productinfo.domain.model;

import java.math.BigDecimal;
import java.util.Objects;

public record PricingInfo(
        boolean available,
        BigDecimal basePrice,
        BigDecimal discountPercent,
        BigDecimal finalPrice,
        String currency,
        String reason
) {

    public static PricingInfo available(BigDecimal basePrice,
                                        BigDecimal discountPercent,
                                        BigDecimal finalPrice,
                                        String currency) {
        Objects.requireNonNull(basePrice, "basePrice");
        Objects.requireNonNull(finalPrice, "finalPrice");
        Objects.requireNonNull(currency, "currency");
        return new PricingInfo(true, basePrice, discountPercent, finalPrice, currency, null);
    }

    public static PricingInfo unavailable(String reason) {
        return new PricingInfo(false, null, null, null, null, reason);
    }
}
