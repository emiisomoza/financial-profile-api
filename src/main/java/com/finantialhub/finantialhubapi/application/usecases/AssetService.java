package com.finantialhub.finantialhubapi.application.usecases;

import com.finantialhub.finantialhubapi.domain.model.Asset;
import com.finantialhub.finantialhubapi.domain.model.AssetType;
import com.finantialhub.finantialhubapi.domain.model.valuation.ValuationMode;
import com.finantialhub.finantialhubapi.infrastructure.persistence.AssetRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Service
public class AssetService {

    private final AssetRepository assetRepository;

    public AssetService(AssetRepository assetRepository) {
        this.assetRepository = assetRepository;
    }

    public Asset createAsset(CreateAssetCommand cmd) {
        AssetType type = AssetType.valueOf(cmd.type());
        ValuationMode valuationMode = ValuationMode.valueOf(cmd.valuationMode());

        Asset asset = Asset.createNew(
                cmd.userId(),
                type,
                cmd.name(),
                cmd.symbol(),
                cmd.quantity(),
                valuationMode,
                cmd.manualUnitValue(),
                cmd.currency()
        );

        return assetRepository.save(asset);
    }

    public List<Asset> getAssetsForUser(UUID userId) {
        return assetRepository.findByUserId(userId);
    }

    // command object from controller to service
    public record CreateAssetCommand(
            UUID userId,
            String type,
            String name,
            String symbol,
            BigDecimal quantity,
            String valuationMode,
            BigDecimal manualUnitValue,
            String currency
    ) {}
}
