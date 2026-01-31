package com.kramp.productinfo.infrastructure.mock.support;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kramp.productinfo.domain.ports.exception.MarketNotFoundException;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.FileNotFoundException;
import java.io.IOException;

@Component
public class MockDataLoader {

    private final ObjectMapper objectMapper;

    public MockDataLoader(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public <T> T load(String path, Class<T> type) {
        try {
            var resource = new ClassPathResource(path);
            if (!resource.exists()) {
                String market = extractMarketFromPath(path);
                throw new MarketNotFoundException(market);
            }
            try (var is = resource.getInputStream()) {
                return objectMapper.readValue(is, type);
            }
        } catch (MarketNotFoundException e) {
            throw e;
        } catch (FileNotFoundException e) {
            String market = extractMarketFromPath(path);
            throw new MarketNotFoundException(market, e);
        } catch (IOException e) {
            throw new RuntimeException("Failed to load mock data from " + path, e);
        }
    }

    /**
     * Extracts market code from path like "mock-data/catalog/fr-FR.json" -> "fr-FR"
     */
    private String extractMarketFromPath(String path) {
        if (path == null) return "unknown";
        int lastSlash = path.lastIndexOf('/');
        int lastDot = path.lastIndexOf('.');
        if (lastSlash >= 0 && lastDot > lastSlash) {
            return path.substring(lastSlash + 1, lastDot);
        }
        return path;
    }
}
