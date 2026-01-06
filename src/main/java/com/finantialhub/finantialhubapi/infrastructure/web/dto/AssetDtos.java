package com.finantialhub.finantialhubapi.infrastructure.web.dto;

import com.finantialhub.finantialhubapi.domain.model.Asset;

import java.math.BigDecimal;

public class AssetDtos {
    public record CreateAssetRequest(
            String userId,
            String type,
            String name,
            String symbol,
            BigDecimal quantity,
            String valuationMode,
            BigDecimal manualUnitValue,
            String currency
    ) {}

    public record AssetResponse(
            String id,
            String userId,
            String type,
            String name,
            String symbol,
            BigDecimal quantity,
            String valuationMode,
            BigDecimal manualUnitValue,
            String currency
    ) {
        public static AssetResponse from(Asset asset) {
            return new AssetResponse(
                    asset.getId().toString(),
                    asset.getUserId().toString(),
                    asset.getType().name(),
                    asset.getName(),
                    asset.getSymbol(),
                    asset.getQuantity(),
                    asset.getValuationMode().name(),
                    asset.getManualUnitValue(),
                    asset.getCurrency()
            );
        }
    }
}
