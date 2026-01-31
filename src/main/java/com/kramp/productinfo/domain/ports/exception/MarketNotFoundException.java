package com.kramp.productinfo.domain.ports.exception;

/**
 * Exception thrown when a requested market is not supported or does not exist.
 */
public class MarketNotFoundException extends RuntimeException {

    private final String market;

    public MarketNotFoundException(String market) {
        super("Market not found: " + market);
        this.market = market;
    }

    public MarketNotFoundException(String market, Throwable cause) {
        super("Market not found: " + market, cause);
        this.market = market;
    }

    public String getMarket() {
        return market;
    }
}
