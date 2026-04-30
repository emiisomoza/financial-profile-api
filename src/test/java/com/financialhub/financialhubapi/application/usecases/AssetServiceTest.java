package com.financialhub.financialhubapi.application.usecases;

import com.financialhub.financialhubapi.domain.exceptions.AssetNotFoundException;
import com.financialhub.financialhubapi.domain.model.Asset;
import com.financialhub.financialhubapi.domain.model.AssetType;
import com.financialhub.financialhubapi.domain.model.valuation.ValuationMode;
import com.financialhub.financialhubapi.infrastructure.persistence.AssetRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AssetServiceTest {

    @Mock
    private AssetRepository assetRepository;

    private AssetService assetService;

    @BeforeEach
    void setUp() {
        assetService = new AssetService(assetRepository);
    }

    @Test
    void createAsset_savesAndReturnsAsset() {
        AssetService.CreateAssetCommand cmd = new AssetService.CreateAssetCommand(
                UUID.randomUUID(),
                "CRYPTO",
                "Bitcoin",
                "BTC",
                new BigDecimal("0.5"),
                "MARKET",
                null,
                "AUD"
        );

        ArgumentCaptor<Asset> captor = ArgumentCaptor.forClass(Asset.class);
        when(assetRepository.save(captor.capture())).thenAnswer(i -> i.getArgument(0));

        Asset result = assetService.createAsset(cmd);

        Asset saved = captor.getValue();
        assertThat(saved.getType()).isEqualTo(AssetType.CRYPTO);
        assertThat(saved.getName()).isEqualTo("Bitcoin");
        assertThat(saved.getSymbol()).isEqualTo("BTC");
        assertThat(saved.getQuantity()).isEqualByComparingTo("0.5");
        assertThat(saved.getValuationMode()).isEqualTo(ValuationMode.MARKET);
        assertThat(saved.getCurrency()).isEqualTo("AUD");
        assertThat(result).isEqualTo(saved);
    }

    @Test
    void updateAsset_updatesFieldsAndSaves() {
        UUID assetId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();

        Asset existing = new Asset(
                assetId,
                userId,
                AssetType.CRYPTO,
                "Bitcoin",
                "BTC",
                new BigDecimal("0.5"),
                ValuationMode.MARKET,
                null,
                "USD",
                Instant.now()
        );

        AssetService.UpdateAssetCommand cmd = new AssetService.UpdateAssetCommand(
                "Bitcoin Updated",
                "BTC",
                new BigDecimal("1.0"),
                "MARKET",
                null,
                "AUD"
        );

        when(assetRepository.findById(assetId)).thenReturn(Optional.of(existing));
        when(assetRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        Asset result = assetService.updateAsset(assetId, cmd);

        assertThat(result.getId()).isEqualTo(assetId);
        assertThat(result.getUserId()).isEqualTo(userId);
        assertThat(result.getType()).isEqualTo(AssetType.CRYPTO); // type is immutable
        assertThat(result.getName()).isEqualTo("Bitcoin Updated");
        assertThat(result.getQuantity()).isEqualByComparingTo("1.0");
        assertThat(result.getCurrency()).isEqualTo("AUD");
    }

    @Test
    void updateAsset_throwsWhenAssetNotFound() {
        UUID assetId = UUID.randomUUID();

        when(assetRepository.findById(assetId)).thenReturn(Optional.empty());

        AssetService.UpdateAssetCommand cmd = new AssetService.UpdateAssetCommand(
                "Bitcoin", "BTC", new BigDecimal("1.0"), "MARKET", null, "AUD"
        );

        assertThrows(AssetNotFoundException.class,
                () -> assetService.updateAsset(assetId, cmd));
    }

    @Test
    void updateAsset_typeRemainsImmutable() {
        UUID assetId = UUID.randomUUID();

        Asset existing = new Asset(
                assetId,
                UUID.randomUUID(),
                AssetType.CRYPTO,
                "Bitcoin",
                "BTC",
                new BigDecimal("0.5"),
                ValuationMode.MARKET,
                null,
                "USD",
                Instant.now()
        );

        when(assetRepository.findById(assetId)).thenReturn(Optional.of(existing));
        when(assetRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        AssetService.UpdateAssetCommand cmd = new AssetService.UpdateAssetCommand(
                "Bitcoin", "BTC", new BigDecimal("1.0"), "MARKET", null, "AUD"
        );

        Asset result = assetService.updateAsset(assetId, cmd);

        assertThat(result.getType()).isEqualTo(AssetType.CRYPTO);
    }

    @Test
    void getAssetsForUser_returnsListFromRepository() {
        UUID userId = UUID.randomUUID();

        Asset asset = new Asset(
                UUID.randomUUID(), userId, AssetType.STOCK, "Apple", "AAPL",
                new BigDecimal("10"), ValuationMode.MARKET, null, "AUD", Instant.now()
        );

        when(assetRepository.findByUserId(userId)).thenReturn(List.of(asset));

        List<Asset> result = assetService.getAssetsForUser(userId);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getSymbol()).isEqualTo("AAPL");
    }
}