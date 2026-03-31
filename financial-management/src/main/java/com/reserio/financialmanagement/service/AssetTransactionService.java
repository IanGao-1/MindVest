package com.reserio.financialmanagement.service;

import com.reserio.financialmanagement.dto.AssetTransactionDTO;
import com.reserio.financialmanagement.model.Asset;
import com.reserio.financialmanagement.model.AssetTransaction;
import com.reserio.financialmanagement.repository.AssetRepository;
import com.reserio.financialmanagement.repository.AssetTransactionRepository;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
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

    @Autowired
    private AccountBalanceService accountBalanceService;

    public List<AssetTransactionDTO> getAllTransactions() {
        return assetTransactionRepository.findAllByOrderByTransactionDateDescIdDesc().stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    public AssetTransactionDTO createTransaction(AssetTransactionDTO transactionDTO) {
        validateTransaction(transactionDTO);

        String normalizedTicker = transactionDTO.getTicker().trim().toUpperCase(Locale.ROOT);
        String transactionType = transactionDTO.getTransactionType().trim().toUpperCase(Locale.ROOT);
        Date transactionDate = transactionDTO.getTransactionDate() != null ? transactionDTO.getTransactionDate() : new Date();

        Asset asset = assetRepository.findByTicker(normalizedTicker);
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

    private Asset applyBuy(Asset asset, AssetTransactionDTO transactionDTO, String ticker, Date transactionDate) {
        BigDecimal transactionAmount = multiply(transactionDTO.getQuantity(), transactionDTO.getPrice());
        accountBalanceService.debit(transactionAmount);

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

        BigDecimal transactionAmount = multiply(transactionDTO.getQuantity(), transactionDTO.getPrice());
        accountBalanceService.credit(transactionAmount);

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
