package com.kramp.productinfo.infrastructure.mock;

import com.kramp.productinfo.domain.model.AvailabilityInfo;
import com.kramp.productinfo.domain.ports.AvailabilityClient;
import com.kramp.productinfo.domain.ports.exception.UpstreamFailureException;
import com.kramp.productinfo.infrastructure.mock.model.AvailabilityDataset;
import com.kramp.productinfo.infrastructure.mock.support.MockDataLoader;
import org.springframework.stereotype.Component;

import static com.kramp.productinfo.infrastructure.mock.MockUpstreamSupport.maybeFail;
import static com.kramp.productinfo.infrastructure.mock.MockUpstreamSupport.simulateLatency;

@Component("mockAvailabilityClient")
public class MockAvailabilityClient implements AvailabilityClient {

    private static final int LATENCY_MS = 100;
    private static final double RELIABILITY = 0.98; // 98%

    private final MockDataLoader loader;

    public MockAvailabilityClient(MockDataLoader loader) {
        this.loader = loader;
    }

    @Override
    public AvailabilityInfo getAvailability(String productId, String market) {
        simulateLatency(LATENCY_MS);
        maybeFail("availability", RELIABILITY);

        AvailabilityDataset dataset = loader.load("mock-data/availability/" + market + ".json", AvailabilityDataset.class);

        AvailabilityDataset.AvailabilityItem item =
                dataset.items() == null ? null : dataset.items().get(productId);

        if (item == null || item.stock() == null) {
            throw new UpstreamFailureException("availability", "NOT_FOUND",
                    "No availability entry for productId=" + productId + ", market=" + market);
        }


        return AvailabilityInfo.known(item.stock(), dataset.warehouse(), item.delivery());
    }
}
