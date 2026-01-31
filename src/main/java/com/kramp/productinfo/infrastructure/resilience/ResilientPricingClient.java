package com.kramp.productinfo.infrastructure.resilience;

import com.kramp.productinfo.domain.model.CustomerContext;
import com.kramp.productinfo.domain.model.PricingInfo;
import com.kramp.productinfo.domain.ports.PricingClient;
import com.kramp.productinfo.domain.ports.exception.UpstreamFailureException;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import io.github.resilience4j.timelimiter.annotation.TimeLimiter;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

/**
 * Resilient wrapper for PricingClient using Resilience4j annotations.
 *
 * Order of decorators (outermost to innermost):
 * Retry -> CircuitBreaker -> TimeLimiter -> actual call
 */
@Component
@Primary
public class ResilientPricingClient implements PricingClient {

    private final PricingClient delegate;

    public ResilientPricingClient(@Qualifier("mockPricingClient") PricingClient delegate) {
        this.delegate = delegate;
    }

    @Override
    public PricingInfo getPricing(String productId, String market, CustomerContext customerContext) {
        try {
            return getPricingWithResilience(productId, market, customerContext).join();
        } catch (CompletionException ex) {
            throw handleException(ex.getCause());
        } catch (Exception ex) {
            throw handleException(ex);
        }
    }

    @Retry(name = "pricing")
    @CircuitBreaker(name = "pricing")
    @TimeLimiter(name = "pricing")
    public CompletableFuture<PricingInfo> getPricingWithResilience(
            String productId, String market, CustomerContext customerContext) {
        return CompletableFuture.supplyAsync(() ->
            delegate.getPricing(productId, market, customerContext)
        );
    }

    private RuntimeException handleException(Throwable ex) {
        if (ex instanceof UpstreamFailureException ufe) {
            return ufe;
        }
        if (ex instanceof java.util.concurrent.TimeoutException) {
            return new UpstreamFailureException("pricing", "TIMEOUT",
                    "Pricing service timed out", ex);
        }
        return new UpstreamFailureException("pricing", "UPSTREAM_ERROR",
                "Resilience wrapper failure: " + rootMessage(ex), ex);
    }

    private static String rootMessage(Throwable t) {
        Throwable cur = t;
        while (cur.getCause() != null) cur = cur.getCause();
        return cur.getMessage();
    }
}
