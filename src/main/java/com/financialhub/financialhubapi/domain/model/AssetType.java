package com.financialhub.financialhubapi.domain.model;

import java.util.Optional;

public enum AssetType {
    PROPERTY(null),
    VEHICLE(null),
    CASH(null),
    STOCK("stock"),
    CRYPTO("crypto"),
    FUND(null),
    OTHER(null);

    private final String priceApiType;

    AssetType(String priceApiType) {
        this.priceApiType = priceApiType;
    }

    public Optional<String> toPriceApiAssetType() {
        return Optional.ofNullable(priceApiType);
    }
}