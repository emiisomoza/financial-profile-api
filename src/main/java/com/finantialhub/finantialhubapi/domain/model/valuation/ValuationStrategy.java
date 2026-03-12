package com.finantialhub.finantialhubapi.domain.model.valuation;

import com.finantialhub.finantialhubapi.domain.model.Asset;
import com.finantialhub.finantialhubapi.domain.ports.MarketPricePort;

import java.math.BigDecimal;
import java.util.Optional;

public interface ValuationStrategy {
    Optional<BigDecimal> resolve(Asset asset, String targetCurrency, MarketPricePort port);
}
