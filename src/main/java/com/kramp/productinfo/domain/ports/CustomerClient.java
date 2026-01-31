package com.kramp.productinfo.domain.ports;

import com.kramp.productinfo.domain.model.CustomerContext;

public interface CustomerClient {

    /**
     * Optional upstream.
     * If this fails (or customerId not provided), the response should be standard/non-personalized.
     */
    CustomerContext getCustomerContext(String customerId, String market);
}
