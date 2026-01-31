package com.kramp.productinfo.infrastructure.mock.model;

import java.util.Map;

public record CustomerDataset(
        Map<String, String> segmentsByCustomerId,
        Map<String, Map<String, String>> preferencesBySegment
) {}
