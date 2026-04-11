package com.finantialhub.finantialhubapi.infrastructure.web;

import com.finantialhub.finantialhubapi.application.usecases.AssetService;
import com.finantialhub.finantialhubapi.application.usecases.AssetService.CreateAssetCommand;
import com.finantialhub.finantialhubapi.domain.model.Asset;
import com.finantialhub.finantialhubapi.infrastructure.security.SecurityUtils;
import com.finantialhub.finantialhubapi.infrastructure.web.dto.AssetDtos.*;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
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
    public ResponseEntity<AssetResponse> createAsset(
            @AuthenticationPrincipal Jwt jwt,
            @RequestBody CreateAssetRequest request) {

        UUID userId = SecurityUtils.resolveUserId(jwt,
                request.userId() != null ? UUID.fromString(request.userId()) : null);

        CreateAssetCommand cmd = new CreateAssetCommand(
                userId,
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
    public List<AssetResponse> listAssets(
            @AuthenticationPrincipal Jwt jwt,
            @RequestParam(required = false) UUID userId) {

        UUID resolvedId = SecurityUtils.resolveUserId(jwt, userId);
        return assetService.getAssetsForUser(resolvedId)
                .stream()
                .map(AssetResponse::from)
                .toList();
    }

    @PutMapping("/{id}")
    public ResponseEntity<AssetResponse> updateAsset(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID id,
            @RequestBody UpdateAssetRequest request) {

        Asset existing = assetService.getAssetById(id);
        SecurityUtils.requireOwnership(jwt, existing.getUserId());

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
