package com.finantialhub.finantialhubapi.domain.model.valuation;

import com.finantialhub.finantialhubapi.domain.model.Asset;
import com.finantialhub.finantialhubapi.domain.ports.MarketPricePort;
import java.math.BigDecimal;
import java.util.Optional;

public class MarketValuationStrategy implements ValuationStrategy {
    @Override
    public Optional<BigDecimal> resolve(Asset asset, String targetCurrency, MarketPricePort port) {
        String priceApiType = asset.getType().toPriceApiAssetType();
        if (priceApiType == null || asset.getSymbol() == null || asset.getSymbol().isBlank()) {
            return Optional.empty();
        }
        return Optional.of(port.getPrice(priceApiType, asset.getSymbol(), targetCurrency));
    }
}
