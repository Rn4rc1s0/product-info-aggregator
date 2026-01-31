package com.kramp.productinfo.domain.model;

import java.util.Map;

public record CustomerContext(
        String customerId,
        String segment,
        Map<String, String> preferences
) {

    public CustomerContext {
        preferences = (preferences == null) ? Map.of() : Map.copyOf(preferences);
    }

    public static CustomerContext standard() {
        return new CustomerContext(null, "STANDARD", Map.of());
    }
}
