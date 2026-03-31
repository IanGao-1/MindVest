package com.reserio.financialmanagement.service;

import com.reserio.financialmanagement.dto.AssetDTO;
import com.reserio.financialmanagement.model.Asset;
import com.reserio.financialmanagement.repository.AssetRepository;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class AssetService {

    @Autowired
    private AssetRepository assetRepository;

    @Transactional
    public List<AssetDTO> getAllAssets() {
        return normalizeAssets(assetRepository.findAll()).stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    public AssetDTO getAssetById(Long id) {
        Asset asset = assetRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Asset not found"));
        return convertToDTO(asset);
    }

    public AssetDTO createAsset(AssetDTO assetDTO) {
        Asset existingAsset = resolveAssetByTicker(assetDTO.getTicker());
        Asset savedAsset;

        if (existingAsset != null) {
            mergeAsset(existingAsset, assetDTO);
            savedAsset = assetRepository.save(existingAsset);
        } else {
            Asset asset = convertToEntity(assetDTO);
            savedAsset = assetRepository.save(asset);
        }

        return convertToDTO(savedAsset);
    }

    @Transactional
    protected Asset resolveAssetByTicker(String ticker) {
        List<Asset> matches = assetRepository.findAllByTickerOrderByIdAsc(ticker);
        if (matches.isEmpty()) {
            return null;
        }
        if (matches.size() == 1) {
            return matches.get(0);
        }

        Asset primary = matches.get(0);
        List<Asset> duplicates = new ArrayList<>(matches.subList(1, matches.size()));

        double totalQuantity = matches.stream()
                .map(Asset::getQuantity)
                .mapToDouble(this::defaultValue)
                .sum();

        BigDecimal totalCost = matches.stream()
                .map(asset -> multiply(asset.getQuantity(), asset.getAvgCost()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        primary.setQuantity(totalQuantity);
        primary.setAvgCost(totalQuantity <= 0 ? 0D : totalCost.divide(BigDecimal.valueOf(totalQuantity), 8, RoundingMode.HALF_UP).doubleValue());

        Asset latestAsset = matches.get(matches.size() - 1);
        primary.setName(primary.getName() != null ? primary.getName() : latestAsset.getName());
        primary.setType(primary.getType() != null ? primary.getType() : latestAsset.getType());
        primary.setCurrentPrice(latestAsset.getCurrentPrice() != null ? latestAsset.getCurrentPrice() : primary.getCurrentPrice());
        primary.setLastUpdated(latestAsset.getLastUpdated() != null ? latestAsset.getLastUpdated() : primary.getLastUpdated());
        primary.setPurchaseDate(primary.getPurchaseDate() != null ? primary.getPurchaseDate() : latestAsset.getPurchaseDate());
        primary.setNotes(primary.getNotes() != null ? primary.getNotes() : latestAsset.getNotes());

        Asset savedPrimary = assetRepository.save(primary);
        assetRepository.deleteAll(duplicates);
        return savedPrimary;
    }

    public AssetDTO updateAsset(Long id, AssetDTO assetDTO) {
        Asset asset = assetRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Asset not found"));
        BeanUtils.copyProperties(assetDTO, asset, "id");
        Asset updatedAsset = assetRepository.save(asset);
        return convertToDTO(updatedAsset);
    }

    public void sellAsset(Long id, Double sellQuantity) {
        Asset asset = assetRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Asset not found"));

        validateSellQuantity(asset, sellQuantity);

        double remainingQuantity = asset.getQuantity() - sellQuantity;
        if (remainingQuantity > 0) {
            asset.setQuantity(remainingQuantity);
            assetRepository.save(asset);
            return;
        }

        assetRepository.deleteById(id);
    }

    public List<AssetDTO> getAssetsByType(String type) {
        return normalizeAssets(assetRepository.findByType(type)).stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    private List<Asset> normalizeAssets(List<Asset> assets) {
        LinkedHashSet<String> tickers = assets.stream()
                .map(Asset::getTicker)
                .filter(ticker -> ticker != null && !ticker.trim().isEmpty())
                .collect(Collectors.toCollection(LinkedHashSet::new));

        List<Asset> normalizedAssets = new ArrayList<>();
        for (String ticker : tickers) {
            Asset normalized = resolveAssetByTicker(ticker);
            if (normalized != null && defaultValue(normalized.getQuantity()) > 0) {
                normalizedAssets.add(normalized);
            }
        }
        return normalizedAssets;
    }

    private AssetDTO convertToDTO(Asset asset) {
        AssetDTO assetDTO = new AssetDTO();
        BeanUtils.copyProperties(asset, assetDTO);
        BigDecimal costBasis = multiply(asset.getQuantity(), asset.getAvgCost());
        BigDecimal currentValue = multiply(asset.getQuantity(), asset.getCurrentPrice());
        BigDecimal unrealizedPnL = currentValue.subtract(costBasis);
        BigDecimal unrealizedPnLRate = costBasis.compareTo(BigDecimal.ZERO) == 0
                ? BigDecimal.ZERO
                : unrealizedPnL.divide(costBasis, 8, RoundingMode.HALF_UP);
        assetDTO.setCostBasis(costBasis.doubleValue());
        assetDTO.setCurrentValue(currentValue.doubleValue());
        assetDTO.setUnrealizedPnL(unrealizedPnL.doubleValue());
        assetDTO.setUnrealizedPnLRate(unrealizedPnLRate.doubleValue());
        return assetDTO;
    }

    private Asset convertToEntity(AssetDTO assetDTO) {
        Asset asset = new Asset();
        BeanUtils.copyProperties(assetDTO, asset);
        return asset;
    }

    private void mergeAsset(Asset existingAsset, AssetDTO incomingAsset) {
        double currentQuantity = defaultValue(existingAsset.getQuantity());
        double incomingQuantity = defaultValue(incomingAsset.getQuantity());
        double mergedQuantity = currentQuantity + incomingQuantity;

        if (mergedQuantity <= 0) {
            throw new IllegalArgumentException("Merged quantity must be greater than 0");
        }

        BigDecimal currentCost = multiply(existingAsset.getQuantity(), existingAsset.getAvgCost());
        BigDecimal incomingCost = multiply(incomingAsset.getQuantity(), incomingAsset.getAvgCost());
        BigDecimal mergedAvgCost = currentCost.add(incomingCost)
                .divide(BigDecimal.valueOf(mergedQuantity), 8, java.math.RoundingMode.HALF_UP);

        existingAsset.setTicker(incomingAsset.getTicker());
        existingAsset.setName(incomingAsset.getName());
        existingAsset.setType(incomingAsset.getType());
        existingAsset.setQuantity(mergedQuantity);
        existingAsset.setAvgCost(mergedAvgCost.doubleValue());
        existingAsset.setCurrentPrice(incomingAsset.getCurrentPrice());
        existingAsset.setPurchaseDate(incomingAsset.getPurchaseDate() != null ? incomingAsset.getPurchaseDate() : existingAsset.getPurchaseDate());
        existingAsset.setLastUpdated(incomingAsset.getLastUpdated() != null ? incomingAsset.getLastUpdated() : existingAsset.getLastUpdated());
        existingAsset.setNotes(incomingAsset.getNotes());
    }

    private BigDecimal multiply(Double quantity, Double price) {
        BigDecimal quantityValue = BigDecimal.valueOf(quantity == null ? 0D : quantity);
        BigDecimal priceValue = BigDecimal.valueOf(price == null ? 0D : price);
        return quantityValue.multiply(priceValue);
    }

    private double defaultValue(Double value) {
        return value == null ? 0D : value;
    }

    private void validateSellQuantity(Asset asset, Double sellQuantity) {
        if (sellQuantity == null || sellQuantity <= 0) {
            throw new IllegalArgumentException("Sell quantity must be greater than 0");
        }

        if (asset.getQuantity() == null || sellQuantity > asset.getQuantity()) {
            throw new IllegalArgumentException("Sell quantity exceeds holding quantity");
        }
    }
}
