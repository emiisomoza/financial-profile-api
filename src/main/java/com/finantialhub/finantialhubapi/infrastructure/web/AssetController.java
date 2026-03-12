package com.finantialhub.finantialhubapi.infrastructure.web;

import com.finantialhub.finantialhubapi.application.usecases.AssetService;
import com.finantialhub.finantialhubapi.application.usecases.AssetService.CreateAssetCommand;
import com.finantialhub.finantialhubapi.domain.model.Asset;
import com.finantialhub.finantialhubapi.infrastructure.web.dto.AssetDtos.*;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/assets")
public class AssetController {

    private final AssetService assetService;

    public AssetController(AssetService assetService) {
        this.assetService = assetService;
    }

    @PostMapping
    public ResponseEntity<AssetResponse> createAsset(@RequestBody CreateAssetRequest request) {
        CreateAssetCommand cmd = new CreateAssetCommand(
                UUID.fromString(request.userId()),
                request.type(),
                request.name(),
                request.symbol(),
                request.quantity(),
                request.valuationMode(),
                request.manualUnitValue(),
                request.currency()
        );

        Asset asset = assetService.createAsset(cmd);

        AssetResponse body = AssetResponse.from(asset);
        URI location = URI.create("/api/v1/assets/" + asset.getId());
        return ResponseEntity.created(location).body(body);
    }

    @GetMapping
    public List<AssetResponse> listAssets(@RequestParam("userId") UUID userId) {
        return assetService.getAssetsForUser(userId)
                .stream()
                .map(AssetResponse::from)
                .toList();
    }

    @PutMapping("/{id}")
    public ResponseEntity<AssetResponse> updateAsset(
            @PathVariable UUID id,
            @RequestBody UpdateAssetRequest request) {

        AssetService.UpdateAssetCommand cmd = new AssetService.UpdateAssetCommand(
                request.name(),
                request.symbol(),
                request.quantity(),
                request.valuationMode(),
                request.manualUnitValue(),
                request.currency()
        );

        Asset updated = assetService.updateAsset(id, cmd);
        return ResponseEntity.ok(AssetResponse.from(updated));
    }
}
