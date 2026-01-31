package com.kramp.productinfo.application;

import com.kramp.productinfo.domain.model.AggregatedProduct;
import com.kramp.productinfo.domain.model.AvailabilityInfo;
import com.kramp.productinfo.domain.model.CustomerContext;
import com.kramp.productinfo.domain.model.PricingInfo;
import com.kramp.productinfo.domain.ports.AvailabilityClient;
import com.kramp.productinfo.domain.ports.CatalogClient;
import com.kramp.productinfo.domain.ports.CustomerClient;
import com.kramp.productinfo.domain.ports.PricingClient;
import com.kramp.productinfo.domain.ports.exception.UpstreamFailureException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;

@Service
public class ProductAggregationService {

    private static final Logger log = LoggerFactory.getLogger(ProductAggregationService.class);

    private final CatalogClient catalogClient;
    private final PricingClient pricingClient;
    private final AvailabilityClient availabilityClient;
    private final CustomerClient customerClient;

    public ProductAggregationService(
            CatalogClient catalogClient,
            PricingClient pricingClient,
            AvailabilityClient availabilityClient,
            CustomerClient customerClient
    ) {
        this.catalogClient = catalogClient;
        this.pricingClient = pricingClient;
        this.availabilityClient = availabilityClient;
        this.customerClient = customerClient;
    }

    /**
     * Aggregates product info for display.
     * Rules:
     * - Catalog is required: if it fails, fail the whole request.
     * - Pricing is optional: if it fails, mark price unavailable.
     * - Availability is optional: if it fails, mark stock unknown.
     * - Customer is optional: if it fails or customerId missing, return standard context.
     */
    public AggregatedProduct aggregate(String productId, String market, String customerId) {
        var product = catalogClient.getProductDetails(productId, market);

        CompletableFuture<CustomerContext> customerFuture =
                CompletableFuture.supplyAsync(() -> resolveCustomerContext(customerId, market));

        CompletableFuture<AvailabilityInfo> availabilityFuture =
                CompletableFuture.supplyAsync(() -> resolveAvailability(productId, market));

        CompletableFuture<PricingInfo> pricingFuture =
                customerFuture.thenApplyAsync(customer -> resolvePricing(productId, market, customer));

        CustomerContext customer = customerFuture.join();
        AvailabilityInfo availability = availabilityFuture.join();
        PricingInfo pricing = pricingFuture.join();

        return new AggregatedProduct(product, pricing, availability, customer);
    }

    private CustomerContext resolveCustomerContext(String customerId, String market) {
        if (customerId == null || customerId.isBlank()) {
            return CustomerContext.standard();
        }
        try {
            return customerClient.getCustomerContext(customerId, market);
        } catch (UpstreamFailureException ex) {
            log.debug("Customer context degraded: service={} reason={} details={}", ex.service(), ex.reason(), ex.details());
            return CustomerContext.standard();
        } catch (Exception ex) {
            log.debug("Customer context degraded: unexpected error", ex);
            return CustomerContext.standard();
        }
    }

    private AvailabilityInfo resolveAvailability(String productId, String market) {
        try {
            return availabilityClient.getAvailability(productId, market);
        } catch (UpstreamFailureException ex) {
            log.debug("Availability degraded: service={} reason={} details={}", ex.service(), ex.reason(), ex.details());
            return AvailabilityInfo.unknown();
        } catch (Exception ex) {
            log.debug("Availability degraded: unexpected error", ex);
            return AvailabilityInfo.unknown();
        }
    }

    private PricingInfo resolvePricing(String productId, String market, CustomerContext customer) {
        try {
            return pricingClient.getPricing(productId, market, customer);
        } catch (UpstreamFailureException ex) {
            log.debug("Pricing degraded: service={} reason={} details={}", ex.service(), ex.reason(), ex.details());
            return PricingInfo.unavailable(ex.reason());
        } catch (Exception ex) {
            log.debug("Pricing degraded: unexpected error", ex);
            return PricingInfo.unavailable("UPSTREAM_ERROR");
        }
    }
}


