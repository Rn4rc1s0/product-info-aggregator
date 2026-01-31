package com.kramp.productinfo.infrastructure.mock;

import com.kramp.productinfo.domain.ports.exception.UpstreamFailureException;
import com.kramp.productinfo.domain.model.CustomerContext;
import com.kramp.productinfo.domain.model.PricingInfo;
import com.kramp.productinfo.domain.ports.PricingClient;
import com.kramp.productinfo.infrastructure.mock.model.PricingDataset;
import com.kramp.productinfo.infrastructure.mock.support.MockDataLoader;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;

import static com.kramp.productinfo.infrastructure.mock.MockUpstreamSupport.maybeFail;
import static com.kramp.productinfo.infrastructure.mock.MockUpstreamSupport.simulateLatency;

@Component("mockPricingClient")
public class MockPricingClient implements PricingClient {

    private static final int LATENCY_MS = 80;
    private static final double RELIABILITY = 0.995; // 99.5%

    private final MockDataLoader loader;

    public MockPricingClient(MockDataLoader loader) {
        this.loader = loader;
    }

    @Override
    public PricingInfo getPricing(String productId, String market, CustomerContext customerContext) {
        simulateLatency(LATENCY_MS);
        maybeFail("pricing", RELIABILITY);

        PricingDataset dataset = loader.load("mock-data/pricing/" + market + ".json", PricingDataset.class);

        PricingDataset.PricingOverride ov =
                dataset.overrides() == null ? null : dataset.overrides().get(productId);

        if (ov != null && Boolean.TRUE.equals(ov.forceUnavailable())) {
            throw new UpstreamFailureException("pricing", ov.reason(), "Forced unavailable via dataset override");
        }


        PricingDataset.PricingItem item =
                dataset.items() == null ? null : dataset.items().get(productId);

        if (item == null || item.basePrice() == null) {
            throw new UpstreamFailureException("pricing", "NOT_FOUND",
                    "No pricing entry for productId=" + productId + ", market=" + market);
        }


        BigDecimal base = item.basePrice();
        BigDecimal discountPercent = discountFor(customerContext.segment());
        BigDecimal multiplier = BigDecimal.ONE.subtract(discountPercent.divide(new BigDecimal("100"), 6, RoundingMode.HALF_UP));
        BigDecimal finalPrice = base.multiply(multiplier).setScale(2, RoundingMode.HALF_UP);

        return PricingInfo.available(base, discountPercent, finalPrice, dataset.currency());
    }

    private BigDecimal discountFor(String segment) {
        if (segment == null) return BigDecimal.ZERO;
        return switch (segment) {
            case "PREMIUM" -> new BigDecimal("12.5");
            case "STANDARD" -> new BigDecimal("5.0");
            case "BASIC" -> new BigDecimal("0.0");
            default -> BigDecimal.ZERO;
        };
    }
}
