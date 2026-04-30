package com.financialhub.financialhubapi.application.usecases;

import com.financialhub.financialhubapi.domain.exceptions.AssetNotFoundException;
import com.financialhub.financialhubapi.domain.model.Asset;
import com.financialhub.financialhubapi.domain.model.AssetType;
import com.financialhub.financialhubapi.domain.model.valuation.ValuationMode;
import com.financialhub.financialhubapi.infrastructure.persistence.AssetRepository;
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

    public Asset getAssetById(UUID assetId) {
        return assetRepository.findById(assetId)
                .orElseThrow(() -> new AssetNotFoundException(assetId));
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

    public Asset updateAsset(UUID assetId, UpdateAssetCommand cmd) {
        Asset existing = assetRepository.findById(assetId)
                .orElseThrow(() -> new AssetNotFoundException(assetId));

        ValuationMode valuationMode = ValuationMode.valueOf(cmd.valuationMode());

        Asset updated = existing.update(
                cmd.name(),
                cmd.symbol(),
                cmd.quantity(),
                valuationMode,
                cmd.manualUnitValue(),
                cmd.currency()
        );

        return assetRepository.save(updated);
    }

    public void deleteAsset(UUID assetId) {
        if (!assetRepository.existsById(assetId)) {
            throw new AssetNotFoundException(assetId);
        }
        assetRepository.deleteById(assetId);
    }

    public record UpdateAssetCommand(
            String name,
            String symbol,
            BigDecimal quantity,
            String valuationMode,
            BigDecimal manualUnitValue,
            String currency
    ) {}
}
