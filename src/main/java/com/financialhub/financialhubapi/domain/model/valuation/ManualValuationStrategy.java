package com.financialhub.financialhubapi.domain.model.valuation;

import com.financialhub.financialhubapi.domain.model.Asset;
import com.financialhub.financialhubapi.domain.ports.MarketPricePort;
import java.math.BigDecimal;
import java.util.Optional;

public class ManualValuationStrategy implements ValuationStrategy {
    @Override
    public Optional<BigDecimal> resolve(Asset asset, String targetCurrency, MarketPricePort port) {
        BigDecimal value = asset.getManualUnitValue() == null ? BigDecimal.ZERO : asset.getManualUnitValue();
        if (targetCurrency.equalsIgnoreCase(asset.getCurrency())) {
            return Optional.of(value);
        }
        BigDecimal fxRate = port.getPrice("fx", asset.getCurrency(), targetCurrency);
        return Optional.of(value.multiply(fxRate));
    }
}
