package com.reserio.financialmanagement.service;

import com.reserio.financialmanagement.dto.AssetDTO;
import com.reserio.financialmanagement.dto.HoldingValueHistoryDTO;
import com.reserio.financialmanagement.dto.MarketHistoryPointDTO;
import com.reserio.financialmanagement.dto.ValueHistoryPointDTO;
import com.reserio.financialmanagement.model.AssetTransaction;
import com.reserio.financialmanagement.repository.AssetTransactionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class PortfolioAnalyticsService {

    @Autowired
    private AssetService assetService;

    @Autowired
    private AssetTransactionRepository assetTransactionRepository;

    @Autowired
    private MarketDataService marketDataService;

    @Autowired
    private AssetTransactionService assetTransactionService;

    public List<HoldingValueHistoryDTO> getHoldingValueHistories() {
        assetTransactionService.normalizeSupportedSingleBuyDates();
        List<AssetDTO> assets = assetService.getAllAssets();
        return assets.stream()
                .filter(asset -> asset.getTicker() != null && asset.getQuantity() != null && asset.getQuantity() > 0)
                .map(this::buildHoldingHistorySafely)
                .filter(java.util.Objects::nonNull)
                .collect(Collectors.toList());
    }

    public List<ValueHistoryPointDTO> getPortfolioValueHistory() {
        assetTransactionService.normalizeSupportedSingleBuyDates();
        Map<String, ValueHistoryPointDTO> merged = new LinkedHashMap<>();

        for (HoldingValueHistoryDTO holdingHistory : getHoldingValueHistories()) {
            for (ValueHistoryPointDTO point : holdingHistory.getPoints()) {
                ValueHistoryPointDTO existing = merged.computeIfAbsent(point.getTimestamp(), key -> {
                    ValueHistoryPointDTO historyPoint = new ValueHistoryPointDTO();
                    historyPoint.setTimestamp(point.getTimestamp());
                    historyPoint.setTimestampValue(point.getTimestampValue());
                    historyPoint.setQuantity(0D);
                    historyPoint.setPrice(null);
                    historyPoint.setCostBasis(0D);
                    historyPoint.setValue(0D);
                    historyPoint.setPnl(0D);
                    historyPoint.setPnlRate(0D);
                    return historyPoint;
                });

                existing.setQuantity(defaultValue(existing.getQuantity()) + defaultValue(point.getQuantity()));
                existing.setCostBasis(defaultValue(existing.getCostBasis()) + defaultValue(point.getCostBasis()));
                existing.setValue(defaultValue(existing.getValue()) + defaultValue(point.getValue()));
                existing.setPnl(defaultValue(existing.getPnl()) + defaultValue(point.getPnl()));
                existing.setPnlRate(calculateRate(existing.getPnl(), existing.getCostBasis()));
            }
        }

        return new ArrayList<>(merged.values());
    }

    private HoldingValueHistoryDTO buildHoldingHistory(AssetDTO asset) {
        List<MarketHistoryPointDTO> marketHistory = marketDataService.getPriceHistory(asset.getTicker());
        List<AssetTransaction> transactions = assetTransactionRepository.findByTickerOrderByTransactionDateAscIdAsc(asset.getTicker());
        transactions.sort(Comparator.comparing(this::transactionEffectiveDay));
        long firstTransactionDay = transactions.isEmpty() ? Long.MIN_VALUE : transactionEffectiveDay(transactions.get(0));

        double runningQuantity = 0D;
        double runningCostBasis = 0D;
        int transactionIndex = 0;
        List<ValueHistoryPointDTO> points = new ArrayList<>();

        for (MarketHistoryPointDTO historyPoint : marketHistory) {
            long pointDay = historyPointDay(historyPoint);
            while (transactionIndex < transactions.size()
                    && transactionEffectiveDay(transactions.get(transactionIndex)) <= pointDay) {
                AssetTransaction transaction = transactions.get(transactionIndex);
                double transactionQuantity = defaultValue(transaction.getQuantity());
                double transactionPrice = transaction.getPrice() == null ? 0D : transaction.getPrice().doubleValue();
                if ("BUY".equalsIgnoreCase(transaction.getTransactionType())) {
                    runningCostBasis += transactionQuantity * transactionPrice;
                    runningQuantity += transactionQuantity;
                } else if ("SELL".equalsIgnoreCase(transaction.getTransactionType())) {
                    double averageCost = runningQuantity > 0 ? runningCostBasis / runningQuantity : 0D;
                    double costReduction = transactionQuantity * averageCost;
                    runningQuantity = Math.max(0D, runningQuantity - transactionQuantity);
                    runningCostBasis = Math.max(0D, runningCostBasis - costReduction);
                }
                transactionIndex++;
            }

            if (pointDay < firstTransactionDay) {
                continue;
            }

            BigDecimal close = historyPoint.getClose() == null ? BigDecimal.ZERO : historyPoint.getClose();
            double value = close.multiply(BigDecimal.valueOf(runningQuantity)).doubleValue();
            double pnl = value - runningCostBasis;
            ValueHistoryPointDTO point = new ValueHistoryPointDTO();
            point.setTimestamp(historyPoint.getTimestamp());
            point.setTimestampValue(historyPoint.getTimestampValue());
            point.setQuantity(runningQuantity);
            point.setPrice(close.doubleValue());
            point.setCostBasis(runningCostBasis);
            point.setValue(value);
            point.setPnl(pnl);
            point.setPnlRate(calculateRate(pnl, runningCostBasis));
            points.add(point);
        }

        HoldingValueHistoryDTO historyDTO = new HoldingValueHistoryDTO();
        historyDTO.setTicker(asset.getTicker());
        historyDTO.setName(asset.getName());
        historyDTO.setPoints(points);
        return historyDTO;
    }

    private HoldingValueHistoryDTO buildHoldingHistorySafely(AssetDTO asset) {
        try {
            return buildHoldingHistory(asset);
        } catch (ResponseStatusException ex) {
            return null;
        }
    }

    private long transactionTime(AssetTransaction transaction) {
        if (transaction.getTransactionDate() == null) {
            return Long.MIN_VALUE;
        }
        return transaction.getTransactionDate().getTime();
    }

    private long transactionEffectiveDay(AssetTransaction transaction) {
        return normalizeToDay(transactionTime(transaction));
    }

    private long historyPointDay(MarketHistoryPointDTO historyPoint) {
        return normalizeToDay(defaultValue(historyPoint.getTimestampValue()));
    }

    private long defaultValue(Long value) {
        return value == null ? Long.MIN_VALUE : value;
    }

    private double defaultValue(Double value) {
        return value == null ? 0D : value;
    }

    private double calculateRate(Double numerator, Double denominator) {
        double base = defaultValue(denominator);
        if (base == 0D) {
            return 0D;
        }
        return defaultValue(numerator) / base;
    }

    private long normalizeToDay(long epochMillis) {
        if (epochMillis == Long.MIN_VALUE) {
            return Long.MIN_VALUE;
        }
        LocalDate localDate = Instant.ofEpochMilli(epochMillis).atZone(ZoneId.of("Asia/Shanghai")).toLocalDate();
        return localDate.atStartOfDay(ZoneId.of("Asia/Shanghai")).toInstant().toEpochMilli();
    }
}
