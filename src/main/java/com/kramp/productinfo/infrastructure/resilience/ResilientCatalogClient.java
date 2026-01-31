package com.kramp.productinfo.infrastructure.resilience;

import com.kramp.productinfo.domain.model.ProductDetails;
import com.kramp.productinfo.domain.ports.CatalogClient;
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
 * Resilient wrapper for CatalogClient using Resilience4j annotations.
 * <p>
 * Order of decorators (outermost to innermost):
 * Retry -> CircuitBreaker -> TimeLimiter -> actual call
 * <p>
 * Catalog is required: failures will propagate and be handled by ControllerAdvice.
 */
@Component
@Primary
public class ResilientCatalogClient implements CatalogClient {

    private final CatalogClient delegate;
    private final ExecutorService upstreamExecutor;

    public ResilientCatalogClient(
            @Qualifier("mockCatalogClient") CatalogClient delegate,
            ExecutorService upstreamExecutor
    ) {
        this.delegate = delegate;
        this.upstreamExecutor = upstreamExecutor;
    }

    @Override
    public ProductDetails getProductDetails(String productId, String market) {
        try {
            return getProductDetailsWithResilience(productId, market).join();
        } catch (CompletionException ex) {
            throw handleException(ex.getCause());
        } catch (Exception ex) {
            throw handleException(ex);
        }
    }

    @Retry(name = "catalog")
    @CircuitBreaker(name = "catalog")
    @TimeLimiter(name = "catalog")
    public CompletableFuture<ProductDetails> getProductDetailsWithResilience(String productId, String market) {
        return CompletableFuture.supplyAsync(() ->
                        delegate.getProductDetails(productId, market),
                upstreamExecutor
        );
    }

    private RuntimeException handleException(Throwable ex) {
        if (ex instanceof UpstreamFailureException ufe) {
            return ufe;
        }
        if (ex instanceof java.util.concurrent.TimeoutException) {
            return new UpstreamFailureException("catalog", "TIMEOUT",
                    "Catalog service timed out", ex);
        }
        return new UpstreamFailureException("catalog", "UPSTREAM_ERROR",
                "Resilience wrapper failure: " + rootMessage(ex), ex);
    }

    private static String rootMessage(Throwable t) {
        Throwable cur = t;
        while (cur.getCause() != null) cur = cur.getCause();
        return cur.getMessage();
    }
}
