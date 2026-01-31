# Product Information Aggregator

A backend service that aggregates product information from multiple internal services (Catalog, Pricing, Availability, Customer) into a single, market-aware response for a B2B e-commerce platform.

## Table of Contents
- [How to Run](#how-to-run)
- [API Documentation](#api-documentation)
- [Architecture Overview](#architecture-overview)
- [Key Design Decisions](#key-design-decisions)
- [Trade-offs](#trade-offs)
- [Production Readiness](#production-readiness)
- [Design Question Answer](#design-question-answer)
- [What I Would Do Differently With More Time](#what-i-would-do-differently-with-more-time)

## How to Run

### Prerequisites
- Java 21 or higher
- Maven 3.8+ (or use the included Maven Wrapper)

### Running the Service

```bash
# Clone the repository
git clone https://github.com/Rn4rc1s0/product-info-aggregator
cd product-info-aggregator

# Build and run
./mvnw spring-boot:run

# Or on Windows
mvnw.cmd spring-boot:run
```

The service will start on `http://localhost:8080`

### Testing the API

```bash
# Basic request (German market, product ABC123)
curl "http://localhost:8080/product-info?productId=ABC123&market=de-DE"

# Request with customer context
curl "http://localhost:8080/product-info?productId=ABC123&market=de-DE&customerId=789"

# Polish market
curl "http://localhost:8080/product-info?productId=ABC123&market=pl-PL&customerId=456"

# Dutch market
curl "http://localhost:8080/product-info?productId=ABC123&market=nl-NL"
```

### API Documentation
After starting the service, access Swagger UI at:
- **Swagger UI**: http://localhost:8080/swagger-ui
- **OpenAPI JSON**: http://localhost:8080/v3/api-docs

### Health & Metrics
- **Health Check**: http://localhost:8080/actuator/health
- **Metrics**: http://localhost:8080/actuator/metrics
- **Circuit Breakers**: http://localhost:8080/actuator/circuitbreakers
- **Circuit Breaker Events**: http://localhost:8080/actuator/circuitbreakerevents
- **Retries**: http://localhost:8080/actuator/retries
- **Retry Events**: http://localhost:8080/actuator/retryevents

## Architecture Overview

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                           Product Info Controller                            │
│                              (REST Endpoint)                                 │
└─────────────────────────────────────────┬───────────────────────────────────┘
                                          │
                                          ▼
┌─────────────────────────────────────────────────────────────────────────────┐
│                         ProductAggregationService                            │
│                    (Orchestrates parallel calls & aggregation)               │
└──────┬──────────────────┬───────────────────┬───────────────────┬───────────┘
       │                  │                   │                   │
       ▼                  ▼                   ▼                   ▼
┌──────────────┐   ┌──────────────┐   ┌──────────────┐   ┌──────────────┐
│   Catalog    │   │   Pricing    │   │ Availability │   │   Customer   │
│    Client    │   │    Client    │   │    Client    │   │    Client    │
│  (REQUIRED)  │   │  (OPTIONAL)  │   │  (OPTIONAL)  │   │  (OPTIONAL)  │
└──────────────┘   └──────────────┘   └──────────────┘   └──────────────┘
       │                  │                   │                   │
       ▼                  ▼                   ▼                   ▼
┌─────────────────────────────────────────────────────────────────────────────┐
│                         Mock Implementations                                 │
│         (Realistic latency simulation + random failure injection)            │
└─────────────────────────────────────────────────────────────────────────────┘
```

### Domain Model

- **ProductDetails**: Core product information (name, description, specs, images) - market-localized
- **PricingInfo**: Base price, discounts, final price, currency - customer-segment aware
- **AvailabilityInfo**: Stock level, warehouse, delivery estimate
- **CustomerContext**: Customer segment and preferences
- **AggregatedProduct**: Combined response containing all the above

### Hexagonal Architecture

The project follows Hexagonal (Ports & Adapters) architecture:

- **Domain Layer** (`domain/model`, `domain/ports`): Core business models and port interfaces
- **Application Layer** (`application/`): Orchestration and business logic
- **Infrastructure Layer** (`infrastructure/mock`): Mock implementations of upstream services
- **Controller Layer** (`controller/`): REST API exposure

This allows easy replacement of mock clients with real HTTP/gRPC clients without changing business logic.

## Key Design Decisions

### 1. Required vs Optional Data Distinction

**Decision**: Catalog is treated as required; all others are optional with graceful degradation.

**Rationale**: 
- Without basic product info (name, description), there's nothing meaningful to display
- A product page with "price unavailable" is better than no page at all
- Users can still add items to cart/wishlist even without real-time stock

**Implementation**:
```java
// Catalog failure → propagate exception → 502 Bad Gateway
// Other failures → return degraded response with flags indicating unavailable data
```

### 2. Parallel Execution Strategy

**Decision**: Execute Catalog first (required), then run Pricing, Availability, and Customer in parallel.

**Rationale**:
- Catalog is required, so we fail fast if it's unavailable
- Other services can run concurrently to minimize total latency
- Pricing depends on CustomerContext for discounts, so we wait for Customer first

**Implementation**: Using `CompletableFuture` with virtual threads for efficient I/O parallelism.

### 3. Virtual Threads (Project Loom)

**Decision**: Enable virtual threads (`spring.threads.virtual.enabled=true`)

**Rationale**:
- Perfect for I/O-bound workloads (calling multiple upstream services)
- Scales to thousands of concurrent requests without thread pool tuning
- Simplifies code (no reactive programming needed for concurrency)

### 4. Resilience Patterns via Resilience4j

**Decision**: Configure Circuit Breaker, Retry, and Timeout per upstream service.

**Configuration Highlights**:
| Service | Timeout | Retry | Circuit Breaker |
|---------|---------|-------|-----------------|
| Catalog | 800ms | 3 attempts | Opens at 50% failure rate |
| Pricing | 500ms | 3 attempts | Opens at 50% failure rate |
| Availability | 600ms | 3 attempts | Opens at 50% failure rate |
| Customer | 400ms | 2 attempts | Opens at 50% failure rate |

**Rationale**: 
- Timeouts prevent cascade failures and ensure reasonable response times
- Circuit breaker prevents hammering failing services
- Retries with exponential backoff handle transient failures without user impact
- `record-exceptions` ensures only relevant failures count toward circuit opening

### 5. Mock Service Realism

**Decision**: Mocks include artificial latency and probabilistic failures matching specs.

| Service | Latency | Reliability |
|---------|---------|-------------|
| Catalog | 50ms | 99.9% |
| Pricing | 80ms | 99.5% |
| Availability | 100ms | 98% |
| Customer | 60ms | 99% |

**Rationale**: Testing with realistic mocks reveals issues that only appear under real-world conditions.

### 6. Market/Language Localization

**Decision**: Each market has separate JSON data files with localized content.

**Rationale**:
- Product names, descriptions in local language
- Market-specific pricing (currency, discounts)
- Regional warehouses and delivery times

## Trade-offs

### 1. Synchronous Catalog Call vs Full Parallelism

**Trade-off**: Catalog is called synchronously before other parallel calls.

**Pros**: 
- Fail fast if catalog unavailable
- Simpler error handling

**Cons**: 
- Slightly higher latency (not fully parallel)
- Could optimize by running all in parallel and canceling on catalog failure

### 2. In-Memory Mock Data vs Embedded Database

**Trade-off**: Using JSON files loaded via ClassPath.

**Pros**: 
- Zero setup required
- Easy to modify test scenarios

**Cons**: 
- Not suitable for testing caching or complex queries
- All data in memory

### 3. Virtual Threads vs WebFlux

**Trade-off**: Chose virtual threads over reactive programming.

**Pros**:
- Simpler, imperative code
- Easier debugging and stack traces
- No reactive learning curve

**Cons**:
- Requires Java 21+
- Less explicit backpressure handling

### 4. Timeout Configuration

**Trade-off**: Aggressive timeouts (250-400ms) for upstream services.

**Pros**:
- Predictable response times for users
- Fast failure detection

**Cons**:
- May reject valid slow responses during network hiccups
- Needs tuning based on real latency distribution

## Production Readiness

### Observability

1. **Health Checks**: `/actuator/health` with liveness and readiness probes
2. **Metrics**: Micrometer integration via `/actuator/metrics`
3. **API Documentation**: OpenAPI/Swagger for API consumers

### Recommended Additions for Production

1. **Distributed Tracing**: Add Spring Cloud Sleuth/Micrometer Tracing for request correlation
2. **Structured Logging**: JSON logging with correlation IDs
3. **Caching**: Add Redis/Caffeine cache for catalog data (changes infrequently)
4. **Rate Limiting**: Add API rate limiting for external clients
5. **Security**: Add OAuth2/JWT authentication
6. **Docker**: Add Dockerfile and docker-compose for containerized deployment

### Error Handling

| Scenario | HTTP Status | Response |
|----------|-------------|----------|
| Product not found | 404 | `{"code": "PRODUCT_NOT_FOUND", "message": "..."}` |
| Catalog service down | 502 | `{"code": "CATALOG_UNAVAILABLE", "message": "..."}` |
| Pricing unavailable | 200 | Product returned with `pricing.available=false` |
| Stock unknown | 200 | Product returned with `availability.stockKnown=false` |
| Invalid request | 400 | `{"code": "INVALID_REQUEST", "message": "..."}` |

## Design Question Answer

### Option A: Adding "Related Products" Service (200ms latency, 90% reliability)

**How would my design accommodate this?**

1. **Create a new port interface**:
```java
public interface RelatedProductsClient {
    List<RelatedProduct> getRelatedProducts(String productId, String market);
}
```

2. **Add to AggregatedProduct model**:
```java
public record AggregatedProduct(
    ProductDetails product,
    PricingInfo pricing,
    AvailabilityInfo availability,
    CustomerContext customer,
    List<RelatedProduct> relatedProducts  // New optional field
) {}
```

3. **Add parallel call in aggregation service**:
```java
CompletableFuture<List<RelatedProduct>> relatedFuture =
    CompletableFuture.supplyAsync(() -> resolveRelatedProducts(productId, market));
```

4. **Configure resilience**:
```yaml
resilience4j:
  timelimiter:
    instances:
      relatedProducts:
        timeout-duration: 350ms  # Higher timeout for 200ms latency
  retry:
    instances:
      relatedProducts:
        max-attempts: 2  # Fewer retries due to low reliability
```

**Should it be required or optional?**

**Optional** — for these reasons:

1. **90% reliability is low**: 1 in 10 requests would fail. Making it required would degrade user experience significantly.

2. **Not critical for purchase decision**: Unlike price and availability, related products are recommendations, not essential information.

3. **Higher latency (200ms)**: Would add to response time. Making it optional allows:
   - Progressive enhancement (show related products when available)
   - Client-side lazy loading

4. **Graceful degradation**: Return product info immediately; show "Related products unavailable" in UI.

**Implementation approach**:
```java
private List<RelatedProduct> resolveRelatedProducts(String productId, String market) {
    try {
        return relatedProductsClient.getRelatedProducts(productId, market);
    } catch (Exception ex) {
        log.warn("Related products unavailable: {}", ex.getMessage());
        return List.of(); // Empty list, not null
    }
}
```

## What I Would Do Differently With More Time

1. **Add comprehensive integration tests**: Test all failure scenarios, timeout behavior, and circuit breaker states.

2. **Implement gRPC endpoint**: As a bonus requirement, add gRPC service definition alongside REST.

3. **Add caching layer**: 
   - Cache catalog data (low change frequency)
   - Short TTL cache for pricing during flash sales
   - Consider cache warming strategies

4. **Enhance observability**:
   - Distributed tracing with trace IDs
   - Custom metrics for upstream latencies
   - Alerting thresholds

5. **Performance testing**: 
   - Load test with Gatling/k6
   - Tune thread pools and connection pools
   - Optimize serialization

6. **Feature flags**: 
   - Gradual rollout of new data sources
   - A/B testing capabilities

7. **Contract testing**: 
   - Pact/Spring Cloud Contract for upstream service contracts
   - Detect breaking changes early

8. **Documentation**:
   - Architecture Decision Records (ADRs)
   - Runbook for operations team
   - Sequence diagrams for complex flows

---

## Supported Markets

| Market Code | Language | Currency |
|-------------|----------|----------|
| de-DE | German | EUR |
| nl-NL | Dutch | EUR |
| pl-PL | Polish | PLN |

## Sample Request/Response

**Request:**
```
GET /product-info?productId=ABC123&market=de-DE&customerId=789
```

**Response:**
```json
{
  "product": {
    "productId": "ABC123",
    "market": "de-DE",
    "name": "Hydraulikschlauch Premium",
    "description": "Hochdruckschlauch für landwirtschaftliche Maschinen...",
    "specs": {
      "length": "2m",
      "diameter": "25mm"
    },
    "imageUrls": ["https://cdn.example.com/images/de/ABC123-1.jpg"]
  },
  "pricing": {
    "available": true,
    "basePrice": 24.90,
    "discountPercent": 5.0,
    "finalPrice": 23.66,
    "currency": "EUR"
  },
  "availability": {
    "stockKnown": true,
    "stockLevel": 8,
    "warehouseCode": "DE-02",
    "expectedDelivery": "1-2 days"
  },
  "customer": {
    "customerId": "789",
    "segment": "STANDARD",
    "preferences": {
      "preferredDelivery": "express",
      "language": "de"
    }
  }
}
```
