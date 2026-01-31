package com.kramp.productinfo.infrastructure.resilience;

import com.kramp.productinfo.domain.model.CustomerContext;
import com.kramp.productinfo.domain.ports.CustomerClient;
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
 * Resilient wrapper for CustomerClient using Resilience4j annotations.
 * <p>
 * Order of decorators (outermost to innermost):
 * Retry -> CircuitBreaker -> TimeLimiter -> actual call
 */
@Component
@Primary
public class ResilientCustomerClient implements CustomerClient {

    private final CustomerClient delegate;
    private final ExecutorService upstreamExecutor;

    public ResilientCustomerClient(
            @Qualifier("mockCustomerClient") CustomerClient delegate,
            ExecutorService upstreamExecutor
    ) {
        this.delegate = delegate;
        this.upstreamExecutor = upstreamExecutor;
    }

    @Override
    public CustomerContext getCustomerContext(String customerId, String market) {
        try {
            return getCustomerContextWithResilience(customerId, market).join();
        } catch (CompletionException ex) {
            throw handleException(ex.getCause());
        } catch (Exception ex) {
            throw handleException(ex);
        }
    }

    @Retry(name = "customer")
    @CircuitBreaker(name = "customer")
    @TimeLimiter(name = "customer")
    public CompletableFuture<CustomerContext> getCustomerContextWithResilience(String customerId, String market) {
        return CompletableFuture.supplyAsync(() ->
                        delegate.getCustomerContext(customerId, market),
                upstreamExecutor
        );
    }

    private RuntimeException handleException(Throwable ex) {
        if (ex instanceof UpstreamFailureException ufe) {
            return ufe;
        }
        if (ex instanceof java.util.concurrent.TimeoutException) {
            return new UpstreamFailureException("customer", "TIMEOUT",
                    "Customer service timed out", ex);
        }
        return new UpstreamFailureException("customer", "UPSTREAM_ERROR",
                "Resilience wrapper failure: " + rootMessage(ex), ex);
    }

    private static String rootMessage(Throwable t) {
        Throwable cur = t;
        while (cur.getCause() != null) cur = cur.getCause();
        return cur.getMessage();
    }
}
