package com.kramp.productinfo.domain.ports;

import com.kramp.productinfo.domain.model.ProductDetails;

public interface CatalogClient {

    /**
     * Required upstream.
     * If this fails, the whole aggregation must fail.
     */
    ProductDetails getProductDetails(String productId, String market);
}
