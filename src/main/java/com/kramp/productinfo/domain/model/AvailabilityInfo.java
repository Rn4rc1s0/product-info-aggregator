package com.kramp.productinfo.domain.model;

public record AvailabilityInfo(
        boolean stockKnown,
        Integer stockLevel,
        String warehouseCode,
        String expectedDelivery
) {

    public static AvailabilityInfo known(int stockLevel, String warehouseCode, String expectedDelivery) {
        return new AvailabilityInfo(true, stockLevel, warehouseCode, expectedDelivery);
    }

    public static AvailabilityInfo unknown() {
        return new AvailabilityInfo(false, null, null, null);
    }
}
