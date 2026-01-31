package com.kramp.productinfo.infrastructure.mock;

import com.kramp.productinfo.domain.model.CustomerContext;
import com.kramp.productinfo.domain.ports.CustomerClient;
import com.kramp.productinfo.domain.ports.exception.UpstreamFailureException;
import com.kramp.productinfo.infrastructure.mock.model.CustomerDataset;
import com.kramp.productinfo.infrastructure.mock.support.MockDataLoader;
import org.springframework.stereotype.Component;

import java.util.Map;

import static com.kramp.productinfo.infrastructure.mock.MockUpstreamSupport.maybeFail;
import static com.kramp.productinfo.infrastructure.mock.MockUpstreamSupport.simulateLatency;

@Component
public class MockCustomerClient implements CustomerClient {

    private static final int LATENCY_MS = 60;
    private static final double RELIABILITY = 0.99; // 99%

    private final MockDataLoader loader;

    public MockCustomerClient(MockDataLoader loader) {
        this.loader = loader;
    }

    @Override
    public CustomerContext getCustomerContext(String customerId, String market) {
        simulateLatency(LATENCY_MS);
        maybeFail("customer", RELIABILITY);

        CustomerDataset dataset = loader.load("mock-data/customer/" + market + ".json", CustomerDataset.class);

        String segment = null;
        if (dataset.segmentsByCustomerId() != null) {
            segment = dataset.segmentsByCustomerId().get(customerId);
        }
        if (segment == null) {
            throw new UpstreamFailureException("customer", "CUSTOMER_NOT_FOUND",
                    "customerId=" + customerId + ", market=" + market);
        }


        Map<String, String> prefs = Map.of();
        if (dataset.preferencesBySegment() != null) {
            prefs = dataset.preferencesBySegment().getOrDefault(segment, Map.of());
        }

        return new CustomerContext(customerId, segment, prefs);
    }
}
