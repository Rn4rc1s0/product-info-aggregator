package com.kramp.productinfo.integration;

import com.kramp.productinfo.domain.model.AggregatedProduct;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for the Product Information Aggregator API.
 * Tests cover happy paths, edge cases, and failure scenarios.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class ProductInfoIntegrationTest {

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    private String baseUrl() {
        return "http://localhost:" + port + "/product-info";
    }

    // ========================================
    // Happy Path Tests
    // ========================================

    @Test
    void shouldReturnAggregatedProduct_whenProductExists_withoutCustomer() {
        ResponseEntity<AggregatedProduct> response = restTemplate.getForEntity(
                baseUrl() + "?productId=ABC123&market=de-DE",
                AggregatedProduct.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();

        AggregatedProduct product = response.getBody();
        // Catalog data
        assertThat(product.product().productId()).isEqualTo("ABC123");
        assertThat(product.product().market()).isEqualTo("de-DE");
        assertThat(product.product().name()).isNotBlank();

        // Pricing should be available
        assertThat(product.pricing().available()).isTrue();
        assertThat(product.pricing().currency()).isEqualTo("EUR");

        // Availability should be known
        assertThat(product.availability().stockKnown()).isTrue();
        assertThat(product.availability().warehouseCode()).isEqualTo("DE-02");

        // Customer should be standard (no customer ID provided)
        assertThat(product.customer().segment()).isEqualTo("STANDARD");
    }

    @Test
    void shouldReturnAggregatedProduct_withCustomerContext() {
        ResponseEntity<AggregatedProduct> response = restTemplate.getForEntity(
                baseUrl() + "?productId=ABC123&market=de-DE&customerId=789",
                AggregatedProduct.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();

        AggregatedProduct product = response.getBody();
        // Customer should be personalized
        assertThat(product.customer().customerId()).isEqualTo("789");
        assertThat(product.customer().segment()).isEqualTo("STANDARD");

        // Pricing should reflect customer discount
        assertThat(product.pricing().available()).isTrue();
        assertThat(product.pricing().discountPercent()).isNotNull();
    }

    @Test
    void shouldReturnLocalizedContent_forPolishMarket() {
        // Using XYZ999 because ABC123 has forceUnavailable pricing in pl-PL
        ResponseEntity<AggregatedProduct> response = restTemplate.getForEntity(
                baseUrl() + "?productId=XYZ999&market=pl-PL",
                AggregatedProduct.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();

        AggregatedProduct product = response.getBody();
        assertThat(product.product().market()).isEqualTo("pl-PL");
        assertThat(product.pricing().available()).isTrue();
        assertThat(product.pricing().currency()).isEqualTo("PLN");
        assertThat(product.availability().warehouseCode()).isEqualTo("PL-01");
    }

    @Test
    void shouldReturnLocalizedContent_forDutchMarket() {
        ResponseEntity<AggregatedProduct> response = restTemplate.getForEntity(
                baseUrl() + "?productId=ABC123&market=nl-NL",
                AggregatedProduct.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();

        AggregatedProduct product = response.getBody();
        assertThat(product.product().market()).isEqualTo("nl-NL");
        assertThat(product.pricing().currency()).isEqualTo("EUR");
        assertThat(product.availability().warehouseCode()).isEqualTo("NL-01");
    }

    // ========================================
    // Customer Discount Tests
    // ========================================

    @Test
    void shouldApplyPremiumDiscount_forPremiumCustomer() {
        // Using XYZ999 in pl-PL because ABC123 has forceUnavailable pricing
        ResponseEntity<AggregatedProduct> response = restTemplate.getForEntity(
                baseUrl() + "?productId=XYZ999&market=pl-PL&customerId=456",
                AggregatedProduct.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        AggregatedProduct product = response.getBody();

        assertThat(product.customer().segment()).isEqualTo("PREMIUM");
        assertThat(product.pricing().available()).isTrue();
        assertThat(product.pricing().discountPercent()).isNotNull();
        // PREMIUM segment gets 12.5% discount
        assertThat(product.pricing().discountPercent().doubleValue()).isEqualTo(12.5);
    }

    @Test
    void shouldApplyStandardDiscount_forStandardCustomer() {
        ResponseEntity<AggregatedProduct> response = restTemplate.getForEntity(
                baseUrl() + "?productId=ABC123&market=de-DE&customerId=789",
                AggregatedProduct.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        AggregatedProduct product = response.getBody();

        assertThat(product.customer().segment()).isEqualTo("STANDARD");
        // STANDARD segment gets 5% discount
        assertThat(product.pricing().discountPercent().doubleValue()).isEqualTo(5.0);
    }

    @Test
    void shouldApplyNoDiscount_forBasicCustomer() {
        ResponseEntity<AggregatedProduct> response = restTemplate.getForEntity(
                baseUrl() + "?productId=ABC123&market=de-DE&customerId=111",
                AggregatedProduct.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        AggregatedProduct product = response.getBody();

        assertThat(product.customer().segment()).isEqualTo("BASIC");
        // BASIC segment gets 0% discount
        assertThat(product.pricing().discountPercent().doubleValue()).isEqualTo(0.0);
    }

    // ========================================
    // Graceful Degradation Tests
    // ========================================

    @Test
    void shouldReturnStandardContext_whenCustomerNotFound() {
        ResponseEntity<AggregatedProduct> response = restTemplate.getForEntity(
                baseUrl() + "?productId=ABC123&market=de-DE&customerId=NONEXISTENT",
                AggregatedProduct.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        AggregatedProduct product = response.getBody();

        // Should fallback to standard context
        assertThat(product.customer().customerId()).isNull();
        assertThat(product.customer().segment()).isEqualTo("STANDARD");
    }

    @Test
    void shouldReturnPricingUnavailable_whenPricingServiceFails() {
        // XYZ999 in nl-NL has forceUnavailable=true with reason "PRICING_ENGINE_MAINTENANCE"
        ResponseEntity<AggregatedProduct> response = restTemplate.getForEntity(
                baseUrl() + "?productId=XYZ999&market=nl-NL",
                AggregatedProduct.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        AggregatedProduct product = response.getBody();

        // Product details should still be returned
        assertThat(product.product().productId()).isEqualTo("XYZ999");
        assertThat(product.product().name()).isNotBlank();

        // Pricing should be marked as unavailable
        assertThat(product.pricing().available()).isFalse();
        assertThat(product.pricing().reason()).isEqualTo("PRICING_ENGINE_MAINTENANCE");

        // Availability should still work
        assertThat(product.availability().stockKnown()).isTrue();
    }

    @Test
    void shouldReturnStockUnknown_whenAvailabilityServiceFails() {
        // ABC123 in pl-PL does not exist in availability data
        ResponseEntity<AggregatedProduct> response = restTemplate.getForEntity(
                baseUrl() + "?productId=ABC123&market=pl-PL",
                AggregatedProduct.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        AggregatedProduct product = response.getBody();

        // Product details should still be returned
        assertThat(product.product().productId()).isEqualTo("ABC123");
        assertThat(product.product().name()).isNotBlank();

        // Availability should be marked as unknown
        assertThat(product.availability().stockKnown()).isFalse();
        assertThat(product.availability().stockLevel()).isNull();
        assertThat(product.availability().warehouseCode()).isNull();
    }

    @Test
    void shouldHandleMultiplePartialFailures_pricingAndAvailability() {
        // ABC123 in pl-PL has BOTH pricing forceUnavailable AND missing availability
        ResponseEntity<AggregatedProduct> response = restTemplate.getForEntity(
                baseUrl() + "?productId=ABC123&market=pl-PL&customerId=456",
                AggregatedProduct.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        AggregatedProduct product = response.getBody();

        // Product details should still be returned (catalog is required and works)
        assertThat(product.product().productId()).isEqualTo("ABC123");

        // Pricing should be unavailable
        assertThat(product.pricing().available()).isFalse();
        assertThat(product.pricing().reason()).isEqualTo("NO_PRICE_FOR_MARKET");

        // Availability should be unknown
        assertThat(product.availability().stockKnown()).isFalse();

        // Customer context should still work
        assertThat(product.customer().segment()).isEqualTo("PREMIUM");
        assertThat(product.customer().customerId()).isEqualTo("456");
    }

    // ========================================
    // Error Scenario Tests
    // ========================================

    @Test
    void shouldReturn404_whenProductNotFound() {
        ResponseEntity<String> response = restTemplate.getForEntity(
                baseUrl() + "?productId=NOTEXIST&market=de-DE",
                String.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody()).contains("PRODUCT_NOT_FOUND");
    }

    @Test
    void shouldReturn400_whenProductIdMissing() {
        ResponseEntity<String> response = restTemplate.getForEntity(
                baseUrl() + "?market=de-DE",
                String.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void shouldReturn400_whenMarketMissing() {
        ResponseEntity<String> response = restTemplate.getForEntity(
                baseUrl() + "?productId=ABC123",
                String.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    // ========================================
    // Data Completeness Tests
    // ========================================

    @Test
    void shouldReturnCompleteProductDetails() {
        ResponseEntity<AggregatedProduct> response = restTemplate.getForEntity(
                baseUrl() + "?productId=ABC123&market=de-DE",
                AggregatedProduct.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        AggregatedProduct product = response.getBody();

        // Verify all product details are present
        assertThat(product.product().name()).isNotBlank();
        assertThat(product.product().description()).isNotBlank();
        assertThat(product.product().specs()).isNotEmpty();
        assertThat(product.product().imageUrls()).isNotEmpty();
    }

    @Test
    void shouldCalculateFinalPriceCorrectly() {
        ResponseEntity<AggregatedProduct> response = restTemplate.getForEntity(
                baseUrl() + "?productId=ABC123&market=de-DE&customerId=789",
                AggregatedProduct.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        AggregatedProduct product = response.getBody();

        // Base price 24.90, 5% discount for STANDARD
        // Expected final: 24.90 * 0.95 = 23.655 -> 23.66 (rounded)
        assertThat(product.pricing().basePrice().doubleValue()).isEqualTo(24.90);
        assertThat(product.pricing().finalPrice().doubleValue()).isEqualTo(23.66);
    }
}
