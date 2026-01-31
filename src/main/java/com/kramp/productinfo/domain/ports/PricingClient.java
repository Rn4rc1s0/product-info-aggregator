package com.kramp.productinfo.domain.ports;

import com.kramp.productinfo.domain.model.CustomerContext;
import com.kramp.productinfo.domain.model.PricingInfo;

public interface PricingClient {
    /**
     * Optional upstream.
     * If this fails, pricing should be marked as unavailable.
     */
    PricingInfo getPricing(String productId, String market, CustomerContext customerContext);
}
