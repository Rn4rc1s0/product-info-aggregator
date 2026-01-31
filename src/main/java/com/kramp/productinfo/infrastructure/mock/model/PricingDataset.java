package com.kramp.productinfo.infrastructure.mock.model;

import java.math.BigDecimal;
import java.util.Map;

public record PricingDataset(
        String currency,
        Map<String, PricingItem> items,
        Map<String, PricingOverride> overrides
) {
    public record PricingItem(BigDecimal basePrice) {}
    public record PricingOverride(Boolean forceUnavailable, String reason) {}
}
