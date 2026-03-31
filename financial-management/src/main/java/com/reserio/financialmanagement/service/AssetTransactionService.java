package com.reserio.financialmanagement.service;

import com.reserio.financialmanagement.dto.AssetTransactionDTO;
import com.reserio.financialmanagement.model.Asset;
import com.reserio.financialmanagement.model.AssetTransaction;
import com.reserio.financialmanagement.repository.AssetRepository;
import com.reserio.financialmanagement.repository.AssetTransactionRepository;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

@Service
public class AssetTransactionService {

    @Autowired
    private AssetTransactionRepository assetTransactionRepository;

    @Autowired
    private AssetRepository assetRepository;

    @Transactional
    public List<AssetTransactionDTO> getAllTransactions() {
        return assetTransactionRepository.findAllByOrderByTransactionDateDescIdDesc().stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    @Transactional
    public AssetTransactionDTO createTransaction(AssetTransactionDTO transactionDTO) {
        validateTransaction(transactionDTO);

        String normalizedTicker = transactionDTO.getTicker().trim().toUpperCase(Locale.ROOT);
        String transactionType = transactionDTO.getTransactionType().trim().toUpperCase(Locale.ROOT);
        Date transactionDate = transactionDTO.getTransactionDate() != null ? transactionDTO.getTransactionDate() : new Date();

        Asset asset = resolveAssetByTicker(normalizedTicker);
        if ("BUY".equals(transactionType)) {
            asset = applyBuy(asset, transactionDTO, normalizedTicker, transactionDate);
        } else {
            asset = applySell(asset, transactionDTO, normalizedTicker, transactionDate);
        }

        AssetTransaction transaction = new AssetTransaction();
        transaction.setAssetId(asset != null ? asset.getId() : null);
        transaction.setTicker(normalizedTicker);
        transaction.setAssetName(resolveAssetName(asset, transactionDTO));
        transaction.setAssetType(resolveAssetType(asset, transactionDTO));
        transaction.setTransactionType(transactionType);
        transaction.setQuantity(transactionDTO.getQuantity());
        transaction.setPrice(transactionDTO.getPrice());
        transaction.setTotalAmount(multiply(transactionDTO.getQuantity(), transactionDTO.getPrice()).doubleValue());
        transaction.setRemainingQuantity(asset != null ? asset.getQuantity() : 0D);
        transaction.setTransactionDate(transactionDate);
        transaction.setNotes(transactionDTO.getNotes());

        AssetTransaction savedTransaction = assetTransactionRepository.save(transaction);
        return convertToDTO(savedTransaction);
    }

    private Asset resolveAssetByTicker(String ticker) {
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

    private Asset applyBuy(Asset asset, AssetTransactionDTO transactionDTO, String ticker, Date transactionDate) {
        BigDecimal transactionAmount = multiply(transactionDTO.getQuantity(), transactionDTO.getPrice());

        if (asset == null) {
            asset = new Asset();
            asset.setTicker(ticker);
            asset.setName(requireText(transactionDTO.getAssetName(), "Asset name is required for a new asset"));
            asset.setType(requireText(transactionDTO.getAssetType(), "Asset type is required for a new asset"));
            asset.setQuantity(0D);
            asset.setAvgCost(0D);
            asset.setPurchaseDate(transactionDate);
        }

        double currentQuantity = defaultValue(asset.getQuantity());
        double buyQuantity = defaultValue(transactionDTO.getQuantity());
        double mergedQuantity = currentQuantity + buyQuantity;

        BigDecimal currentCost = multiply(asset.getQuantity(), asset.getAvgCost());
        BigDecimal mergedAvgCost = currentCost.add(transactionAmount)
                .divide(BigDecimal.valueOf(mergedQuantity), 8, RoundingMode.HALF_UP);

        asset.setName(firstNonBlank(transactionDTO.getAssetName(), asset.getName()));
        asset.setType(firstNonBlank(transactionDTO.getAssetType(), asset.getType()));
        asset.setQuantity(mergedQuantity);
        asset.setAvgCost(mergedAvgCost.doubleValue());
        asset.setCurrentPrice(resolveCurrentPriceForBuy(asset, transactionDTO));
        asset.setPurchaseDate(asset.getPurchaseDate() == null ? transactionDate : asset.getPurchaseDate());
        asset.setLastUpdated(new Date());
        asset.setNotes(firstNonBlank(transactionDTO.getNotes(), asset.getNotes()));
        return assetRepository.save(asset);
    }

    private Asset applySell(Asset asset, AssetTransactionDTO transactionDTO, String ticker, Date transactionDate) {
        if (asset == null) {
            throw new IllegalArgumentException("Asset not found for ticker: " + ticker);
        }

        double sellQuantity = defaultValue(transactionDTO.getQuantity());
        double currentQuantity = defaultValue(asset.getQuantity());
        if (sellQuantity <= 0) {
            throw new IllegalArgumentException("Transaction quantity must be greater than 0");
        }
        if (sellQuantity > currentQuantity) {
            throw new IllegalArgumentException("Sell quantity exceeds holding quantity");
        }

        double remainingQuantity = currentQuantity - sellQuantity;
        if (remainingQuantity <= 0D) {
            assetRepository.delete(asset);
            asset.setQuantity(0D);
            asset.setCurrentPrice(resolveCurrentPriceForSell(asset, transactionDTO));
            asset.setLastUpdated(transactionDate);
            return asset;
        }

        asset.setQuantity(remainingQuantity);
        asset.setCurrentPrice(resolveCurrentPriceForSell(asset, transactionDTO));
        asset.setLastUpdated(new Date());
        return assetRepository.save(asset);
    }

    private AssetTransactionDTO convertToDTO(AssetTransaction transaction) {
        AssetTransactionDTO dto = new AssetTransactionDTO();
        BeanUtils.copyProperties(transaction, dto);
        return dto;
    }

    private void validateTransaction(AssetTransactionDTO transactionDTO) {
        if (transactionDTO.getTicker() == null || transactionDTO.getTicker().trim().isEmpty()) {
            throw new IllegalArgumentException("Ticker is required");
        }
        if (transactionDTO.getTransactionType() == null || transactionDTO.getTransactionType().trim().isEmpty()) {
            throw new IllegalArgumentException("Transaction type is required");
        }

        String transactionType = transactionDTO.getTransactionType().trim().toUpperCase(Locale.ROOT);
        if (!"BUY".equals(transactionType) && !"SELL".equals(transactionType)) {
            throw new IllegalArgumentException("Transaction type must be BUY or SELL");
        }
        if (transactionDTO.getQuantity() == null || transactionDTO.getQuantity() <= 0) {
            throw new IllegalArgumentException("Transaction quantity must be greater than 0");
        }
        if (transactionDTO.getPrice() == null || transactionDTO.getPrice() <= 0) {
            throw new IllegalArgumentException("Transaction price must be greater than 0");
        }
    }

    private String resolveAssetName(Asset asset, AssetTransactionDTO transactionDTO) {
        return asset != null ? asset.getName() : firstNonBlank(transactionDTO.getAssetName(), transactionDTO.getTicker());
    }

    private String resolveAssetType(Asset asset, AssetTransactionDTO transactionDTO) {
        return asset != null ? asset.getType() : transactionDTO.getAssetType();
    }

    private Double resolveCurrentPriceForBuy(Asset asset, AssetTransactionDTO transactionDTO) {
        if (transactionDTO.getCurrentPrice() != null && transactionDTO.getCurrentPrice() > 0) {
            return transactionDTO.getCurrentPrice();
        }
        if (asset.getCurrentPrice() != null && asset.getCurrentPrice() > 0) {
            return asset.getCurrentPrice();
        }
        return transactionDTO.getPrice();
    }

    private Double resolveCurrentPriceForSell(Asset asset, AssetTransactionDTO transactionDTO) {
        if (transactionDTO.getCurrentPrice() != null && transactionDTO.getCurrentPrice() > 0) {
            return transactionDTO.getCurrentPrice();
        }
        if (asset.getCurrentPrice() != null && asset.getCurrentPrice() > 0) {
            return asset.getCurrentPrice();
        }
        return transactionDTO.getPrice();
    }

    private String requireText(String value, String message) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException(message);
        }
        return value.trim();
    }

    private String firstNonBlank(String first, String fallback) {
        if (first != null && !first.trim().isEmpty()) {
            return first.trim();
        }
        return fallback;
    }

    private BigDecimal multiply(Double quantity, Double price) {
        BigDecimal quantityValue = BigDecimal.valueOf(defaultValue(quantity));
        BigDecimal priceValue = BigDecimal.valueOf(defaultValue(price));
        return quantityValue.multiply(priceValue);
    }

    private double defaultValue(Double value) {
        return value == null ? 0D : value;
    }
}
