package com.kramp.productinfo.infrastructure.resilience;

import com.kramp.productinfo.domain.model.AvailabilityInfo;
import com.kramp.productinfo.domain.ports.AvailabilityClient;
import com.kramp.productinfo.domain.ports.exception.UpstreamFailureException;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import io.github.resilience4j.timelimiter.annotation.TimeLimiter;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutorService;

/**
 * Resilient wrapper for AvailabilityClient using Resilience4j annotations.
 * <p>
 * Order of decorators (outermost to innermost):
 * Retry -> CircuitBreaker -> TimeLimiter -> actual call
 */
@Component
@Primary
public class ResilientAvailabilityClient implements AvailabilityClient {

    private final AvailabilityClient delegate;
    private final ExecutorService upstreamExecutor;

    public ResilientAvailabilityClient(
            @Qualifier("mockAvailabilityClient") AvailabilityClient delegate,
            ExecutorService upstreamExecutor
    ) {
        this.delegate = delegate;
        this.upstreamExecutor = upstreamExecutor;
    }

    @Override
    public AvailabilityInfo getAvailability(String productId, String market) {
        try {
            return getAvailabilityWithResilience(productId, market).join();
        } catch (CompletionException ex) {
            throw handleException(ex.getCause());
        } catch (Exception ex) {
            throw handleException(ex);
        }
    }

    @Retry(name = "availability")
    @CircuitBreaker(name = "availability")
    @TimeLimiter(name = "availability")
    public CompletableFuture<AvailabilityInfo> getAvailabilityWithResilience(String productId, String market) {
        return CompletableFuture.supplyAsync(() ->
                        delegate.getAvailability(productId, market),
                upstreamExecutor
        );
    }

    private RuntimeException handleException(Throwable ex) {
        if (ex instanceof UpstreamFailureException ufe) {
            return ufe;
        }
        if (ex instanceof java.util.concurrent.TimeoutException) {
            return new UpstreamFailureException("availability", "TIMEOUT",
                    "Availability service timed out", ex);
        }
        return new UpstreamFailureException("availability", "UPSTREAM_ERROR",
                "Resilience wrapper failure: " + rootMessage(ex), ex);
    }

    private static String rootMessage(Throwable t) {
        Throwable cur = t;
        while (cur.getCause() != null) cur = cur.getCause();
        return cur.getMessage();
    }
}
