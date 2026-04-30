package com.financialhub.financialhubapi.domain.model.valuation;

import com.financialhub.financialhubapi.domain.model.Asset;
import com.financialhub.financialhubapi.domain.ports.MarketPricePort;
import java.math.BigDecimal;
import java.util.Optional;

public class MarketValuationStrategy implements ValuationStrategy {
    @Override
    public Optional<BigDecimal> resolve(Asset asset, String targetCurrency, MarketPricePort port) {
        return asset.getType()
                .toPriceApiAssetType()
                .filter(type -> asset.getSymbol() != null && !asset.getSymbol().isBlank())
                .map(type -> port.getPrice(type, asset.getSymbol(), targetCurrency));
    }
}
