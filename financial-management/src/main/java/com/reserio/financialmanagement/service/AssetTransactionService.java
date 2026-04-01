package com.reserio.financialmanagement.service;

import com.reserio.financialmanagement.dto.AssetTransactionDTO;
import com.reserio.financialmanagement.dto.MarketHistoryPointDTO;
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
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class AssetTransactionService {
    private static final Map<String, String> DEFAULT_ASSET_NAMES = Arrays.stream(new String[][]{
            {"AAPL", "Apple Inc."},
            {"TSLA", "Tesla Inc."},
            {"MSFT", "Microsoft Corp."},
            {"AMZN", "Amazon.com Inc."},
            {"META", "Meta Platforms Inc."},
            {"NVDA", "NVIDIA Corp."},
            {"C", "Citigroup Inc."}
    }).collect(Collectors.toMap(values -> values[0], values -> values[1]));

    @Autowired
    private AssetTransactionRepository assetTransactionRepository;

    @Autowired
    private AssetRepository assetRepository;

    @Autowired
    private MarketDataService marketDataService;

    @Transactional
    public List<AssetTransactionDTO> getAllTransactions() {
        normalizeSupportedSingleBuyDates();
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

    @Transactional
    public void normalizeSupportedSingleBuyDates() {
        Map<String, List<AssetTransaction>> transactionsByTicker = assetTransactionRepository.findAll().stream()
                .filter(transaction -> transaction.getTicker() != null && DEFAULT_ASSET_NAMES.containsKey(transaction.getTicker()))
                .collect(Collectors.groupingBy(AssetTransaction::getTicker, LinkedHashMap::new, Collectors.toList()));

        for (Map.Entry<String, List<AssetTransaction>> entry : transactionsByTicker.entrySet()) {
            List<AssetTransaction> tickerTransactions = entry.getValue();
            if (tickerTransactions.size() != 1) {
                continue;
            }

            AssetTransaction transaction = tickerTransactions.get(0);
            if (!"BUY".equalsIgnoreCase(transaction.getTransactionType())) {
                continue;
            }

            try {
                List<MarketHistoryPointDTO> history = marketDataService.getPriceHistory(entry.getKey());
                if (history.isEmpty() || history.get(0).getTimestampValue() == null) {
                    continue;
                }

                Date earliestDate = new Date(history.get(0).getTimestampValue());
                if (sameOrBefore(transaction.getTransactionDate(), earliestDate)) {
                    continue;
                }

                transaction.setTransactionDate(earliestDate);
                assetTransactionRepository.save(transaction);

                List<Asset> assets = assetRepository.findAllByTickerOrderByIdAsc(entry.getKey());
                for (Asset asset : assets) {
                    asset.setPurchaseDate(earliestDate);
                }
                if (!assets.isEmpty()) {
                    assetRepository.saveAll(assets);
                }
            } catch (org.springframework.web.server.ResponseStatusException ignored) {
            }
        }
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
            asset.setName(resolveDefaultAssetName(ticker, transactionDTO.getAssetName()));
            asset.setType(resolveDefaultAssetType(transactionDTO.getAssetType()));
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
        return asset != null ? asset.getType() : resolveDefaultAssetType(transactionDTO.getAssetType());
    }

    private Double resolveCurrentPriceForBuy(Asset asset, AssetTransactionDTO transactionDTO) {
        if (transactionDTO.getCurrentPrice() != null && transactionDTO.getCurrentPrice() > 0) {
            return transactionDTO.getCurrentPrice();
        }
        try {
            return marketDataService.getMarketPrice(asset.getTicker());
        } catch (Exception ignored) {
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
        try {
            return marketDataService.getMarketPrice(asset.getTicker());
        } catch (Exception ignored) {
        }
        if (asset.getCurrentPrice() != null && asset.getCurrentPrice() > 0) {
            return asset.getCurrentPrice();
        }
        return transactionDTO.getPrice();
    }

    private String firstNonBlank(String first, String fallback) {
        if (first != null && !first.trim().isEmpty()) {
            return first.trim();
        }
        return fallback;
    }

    private String resolveDefaultAssetName(String ticker, String providedName) {
        if (providedName != null && !providedName.trim().isEmpty()) {
            return providedName.trim();
        }
        return DEFAULT_ASSET_NAMES.getOrDefault(ticker, ticker);
    }

    private String resolveDefaultAssetType(String providedType) {
        if (providedType != null && !providedType.trim().isEmpty()) {
            return providedType.trim().toUpperCase(Locale.ROOT);
        }
        return "STOCK";
    }

    private BigDecimal multiply(Double quantity, Double price) {
        BigDecimal quantityValue = BigDecimal.valueOf(defaultValue(quantity));
        BigDecimal priceValue = BigDecimal.valueOf(defaultValue(price));
        return quantityValue.multiply(priceValue);
    }

    private double defaultValue(Double value) {
        return value == null ? 0D : value;
    }

    private boolean sameOrBefore(Date value, Date reference) {
        if (value == null) {
            return false;
        }
        LocalDate currentDate = Instant.ofEpochMilli(value.getTime()).atZone(ZoneId.of("Asia/Shanghai")).toLocalDate();
        LocalDate referenceDate = Instant.ofEpochMilli(reference.getTime()).atZone(ZoneId.of("Asia/Shanghai")).toLocalDate();
        return !currentDate.isAfter(referenceDate);
    }
}
