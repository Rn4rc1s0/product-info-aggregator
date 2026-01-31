package com.kramp.productinfo.infrastructure.mock;

import com.kramp.productinfo.domain.model.ProductDetails;
import com.kramp.productinfo.domain.ports.CatalogClient;
import com.kramp.productinfo.domain.ports.exception.UpstreamFailureException;
import com.kramp.productinfo.infrastructure.mock.model.CatalogDataset;
import com.kramp.productinfo.infrastructure.mock.support.MockDataLoader;
import org.springframework.stereotype.Component;

import java.util.Map;

import static com.kramp.productinfo.infrastructure.mock.MockUpstreamSupport.maybeFail;
import static com.kramp.productinfo.infrastructure.mock.MockUpstreamSupport.simulateLatency;

@Component
public class MockCatalogClient implements CatalogClient {

    private static final int LATENCY_MS = 50;
    private static final double RELIABILITY = 0.999;

    private final MockDataLoader loader;

    public MockCatalogClient(MockDataLoader loader) {
        this.loader = loader;
    }

    @Override
    public ProductDetails getProductDetails(String productId, String market) {
        simulateLatency(LATENCY_MS);
        maybeFail("catalog", RELIABILITY);

        CatalogDataset dataset = loader.load("mock-data/catalog/" + market + ".json", CatalogDataset.class);
        CatalogDataset.CatalogProduct p = dataset.products().get(productId);

        if (p == null) {
            throw new UpstreamFailureException("catalog", "PRODUCT_NOT_FOUND",
                    "productId=" + productId + ", market=" + market);
        }

        return new ProductDetails(
                productId,
                market,
                p.name(),
                p.description(),
                p.specs() == null ? Map.of() : p.specs(),
                p.images()
        );
    }
}
