package com.financialhub.financialhubapi.domain.model.valuation;

import com.financialhub.financialhubapi.domain.model.Asset;
import com.financialhub.financialhubapi.domain.ports.MarketPricePort;

import java.math.BigDecimal;
import java.util.Optional;

public interface ValuationStrategy {
    Optional<BigDecimal> resolve(Asset asset, String targetCurrency, MarketPricePort port);
}
