package com.kramp.productinfo.domain.model;

import java.util.List;
import java.util.Map;

public record ProductDetails(
        String productId,
        String market,
        String name,
        String description,
        Map<String, String> specs,
        List<String> imageUrls
) {
    public ProductDetails {
        specs = (specs == null) ? Map.of() : Map.copyOf(specs);
        imageUrls = (imageUrls == null) ? List.of() : List.copyOf(imageUrls);
    }
}
