package com.kramp.productinfo.domain.ports;

import com.kramp.productinfo.domain.model.AvailabilityInfo;

public interface AvailabilityClient {

    /**
     * Optional upstream.
     * If this fails, stock should be marked as unknown.
     */
    AvailabilityInfo getAvailability(String productId, String market);
}
