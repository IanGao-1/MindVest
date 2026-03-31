package com.reserio.financialmanagement.controller;

import com.reserio.financialmanagement.dto.AssetDTO;
import com.reserio.financialmanagement.dto.SellAssetRequest;
import com.reserio.financialmanagement.service.AssetService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/assets")
@Tag(name = "Asset", description = "Asset management API")
public class AssetController {

    @Autowired
    private AssetService assetService;

    @Operation(summary = "Get all assets")
    @GetMapping
    public ResponseEntity<List<AssetDTO>> getAllAssets() {
        List<AssetDTO> assets = assetService.getAllAssets();
        return ResponseEntity.ok(assets);
    }

    @Operation(summary = "Get asset by ID")
    @GetMapping("/{id}")
    public ResponseEntity<AssetDTO> getAssetById(@PathVariable Long id) {
        AssetDTO asset = assetService.getAssetById(id);
        return ResponseEntity.ok(asset);
    }

    @Operation(summary = "Create asset")
    @PostMapping
    public ResponseEntity<AssetDTO> createAsset(@RequestBody AssetDTO assetDTO) {
        AssetDTO createdAsset = assetService.createAsset(assetDTO);
        return ResponseEntity.status(HttpStatus.CREATED).body(createdAsset);
    }

    @Operation(summary = "Update asset")
    @PutMapping("/{id}")
    public ResponseEntity<AssetDTO> updateAsset(@PathVariable Long id, @RequestBody AssetDTO assetDTO) {
        AssetDTO updatedAsset = assetService.updateAsset(id, assetDTO);
        return ResponseEntity.ok(updatedAsset);
    }

    @Operation(summary = "Sell asset by quantity")
    @PostMapping("/{id}/sell")
    public ResponseEntity<Void> sellAsset(@PathVariable Long id, @RequestBody SellAssetRequest request) {
        assetService.sellAsset(id, request.getQuantity());
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Get assets by type")
    @GetMapping("/type/{type}")
    public ResponseEntity<List<AssetDTO>> getAssetsByType(@PathVariable String type) {
        List<AssetDTO> assets = assetService.getAssetsByType(type);
        return ResponseEntity.ok(assets);
    }
}
