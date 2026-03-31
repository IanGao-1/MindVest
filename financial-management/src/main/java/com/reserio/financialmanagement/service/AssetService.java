package com.reserio.financialmanagement.service;

import com.reserio.financialmanagement.dto.AssetDTO;
import com.reserio.financialmanagement.model.Asset;
import com.reserio.financialmanagement.repository.AssetRepository;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class AssetService {

    @Autowired
    private AssetRepository assetRepository;

    public List<AssetDTO> getAllAssets() {
        return assetRepository.findAll().stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    public AssetDTO getAssetById(Long id) {
        Asset asset = assetRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Asset not found"));
        return convertToDTO(asset);
    }

    public AssetDTO createAsset(AssetDTO assetDTO) {
        Asset asset = convertToEntity(assetDTO);
        Asset savedAsset = assetRepository.save(asset);
        return convertToDTO(savedAsset);
    }

    public AssetDTO updateAsset(Long id, AssetDTO assetDTO) {
        Asset asset = assetRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Asset not found"));
        BeanUtils.copyProperties(assetDTO, asset, "id");
        Asset updatedAsset = assetRepository.save(asset);
        return convertToDTO(updatedAsset);
    }

    public void deleteAsset(Long id) {
        assetRepository.deleteById(id);
    }

    public List<AssetDTO> getAssetsByType(String type) {
        return assetRepository.findByType(type).stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    private AssetDTO convertToDTO(Asset asset) {
        AssetDTO assetDTO = new AssetDTO();
        BeanUtils.copyProperties(asset, assetDTO);
        return assetDTO;
    }

    private Asset convertToEntity(AssetDTO assetDTO) {
        Asset asset = new Asset();
        BeanUtils.copyProperties(assetDTO, asset);
        return asset;
    }
}