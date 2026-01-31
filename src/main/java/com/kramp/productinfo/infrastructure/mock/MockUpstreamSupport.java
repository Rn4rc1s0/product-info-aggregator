package com.kramp.productinfo.infrastructure.mock;

import com.kramp.productinfo.domain.ports.exception.UpstreamFailureException;

import java.util.concurrent.ThreadLocalRandom;

public final class MockUpstreamSupport {
    private MockUpstreamSupport() {
    }

    public static void simulateLatency(int ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new UpstreamFailureException("internal", "INTERRUPTED", "Interrupted while simulating latency", e);
        }
    }

    public static void maybeFail(String serviceName, double reliability) {
        if (ThreadLocalRandom.current().nextDouble() > reliability) {
            throw new UpstreamFailureException(serviceName, "SIMULATED_FAILURE", "Random failure based on reliability");
        }
    }
}
