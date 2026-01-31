package com.kramp.productinfo.infrastructure.mock.model;

import java.util.Map;

public record AvailabilityDataset(
        String warehouse,
        Map<String, AvailabilityItem> items
) {
    public record AvailabilityItem(Integer stock, String delivery) {}
}
