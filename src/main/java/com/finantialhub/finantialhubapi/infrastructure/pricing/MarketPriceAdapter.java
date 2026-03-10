package com.finantialhub.finantialhubapi.infrastructure.pricing;

import com.finantialhub.finantialhubapi.domain.ports.MarketPricePort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class MarketPriceAdapter implements MarketPricePort {

    private static final Logger log = LoggerFactory.getLogger(MarketPriceAdapter.class);

    private final PriceApiClient priceApiClient;

    public MarketPriceAdapter(PriceApiClient priceApiClient) {
        this.priceApiClient = priceApiClient;
    }

    @Override
    public BigDecimal getPrice(String assetType, String symbol, String targetCurrency) {
        log.info("Fetching price for assetType={} symbol={} currency={}", assetType, symbol, targetCurrency);
        try {
            return priceApiClient.fetchPrice(assetType, symbol, targetCurrency);
        } catch (Exception e) {
            log.error("Failed to fetch price for {}/{}: {}", symbol, targetCurrency, e.getMessage());
            throw new RuntimeException("Price unavailable for %s in %s".formatted(symbol, targetCurrency), e);
        }
    }
}