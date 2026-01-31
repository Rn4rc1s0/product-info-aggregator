package com.kramp.productinfo.infrastructure.mock.model;

import java.util.List;
import java.util.Map;

public record CatalogDataset(
        Map<String, CatalogProduct> products
) {
    public record CatalogProduct(
            String name,
            String description,
            Map<String, String> specs,
            List<String> images
    ) {}
}
