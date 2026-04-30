package com.financialhub.financialhubapi.domain.ports;

import java.math.BigDecimal;

public interface MarketPricePort {

    /**
     * Returns the unit price of an asset in the target currency.
     * Examples:
     *   getPrice("fx",     "USD",  "AUD") → 1.55
     *   getPrice("crypto", "BTC",  "AUD") → 98000.00
     *   getPrice("stock",  "AAPL", "AUD") → 278.45
     */
    BigDecimal getPrice(String assetType, String symbol, String targetCurrency);
}