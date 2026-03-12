package com.finantialhub.finantialhubapi.domain.model.valuation;

public enum ValuationMode {
    MANUAL(new ManualValuationStrategy()),
    MARKET(new MarketValuationStrategy());

    private final ValuationStrategy strategy;

    ValuationMode(ValuationStrategy strategy) {
        this.strategy = strategy;
    }

    public ValuationStrategy strategy() {
        return strategy;
    }
}
