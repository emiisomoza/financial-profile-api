package com.finantialhub.finantialhubapi.domain.model;

public enum AssetType {
    PROPERTY,
    VEHICLE,
    CASH,
    STOCK,
    CRYPTO,
    FUND,
    OTHER;

    public String toPriceApiAssetType() {
        return switch (this) {
            case STOCK -> "stock";
            case CRYPTO -> "crypto";
            default -> null;
        };
    }

    public boolean hasMarketPrice() {
        return toPriceApiAssetType() != null;
    }
}