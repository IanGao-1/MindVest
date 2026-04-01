package com.reserio.financialmanagement.service;

import com.reserio.financialmanagement.dto.AssetTransactionDTO;
import com.reserio.financialmanagement.dto.MarketHistoryPointDTO;
import com.reserio.financialmanagement.model.Asset;
import com.reserio.financialmanagement.model.AssetTransaction;
import com.reserio.financialmanagement.repository.AssetRepository;
import com.reserio.financialmanagement.repository.AssetTransactionRepository;
import com.reserio.financialmanagement.repository.PortfolioAssetRepository;
import com.reserio.financialmanagement.repository.PortfolioRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class SampleDataService {
    private static final Set<String> SUPPORTED_TICKERS =
            Arrays.stream(new String[]{"AAPL", "TSLA", "AMZN", "MSFT", "META", "NVDA", "C"})
                    .collect(Collectors.toSet());

    private static final LocalDate START_DATE = LocalDate.of(2026, 3, 27);
    private static final Date START_DATE_TIME = Date.from(
            LocalDateTime.of(2026, 3, 27, 9, 30)
                    .atZone(ZoneId.of("Asia/Shanghai"))
                    .toInstant()
    );

    private final AssetRepository assetRepository;
    private final AssetTransactionRepository assetTransactionRepository;
    private final PortfolioRepository portfolioRepository;
    private final PortfolioAssetRepository portfolioAssetRepository;
    private final AssetTransactionService assetTransactionService;
    private final MarketDataService marketDataService;

    public SampleDataService(
            AssetRepository assetRepository,
            AssetTransactionRepository assetTransactionRepository,
            PortfolioRepository portfolioRepository,
            PortfolioAssetRepository portfolioAssetRepository,
            AssetTransactionService assetTransactionService,
            MarketDataService marketDataService
    ) {
        this.assetRepository = assetRepository;
        this.assetTransactionRepository = assetTransactionRepository;
        this.portfolioRepository = portfolioRepository;
        this.portfolioAssetRepository = portfolioAssetRepository;
        this.assetTransactionService = assetTransactionService;
        this.marketDataService = marketDataService;
    }

    @Transactional
    public void resetToStarterData() {
        portfolioAssetRepository.deleteAll();
        portfolioRepository.deleteAll();
        assetTransactionRepository.deleteAll();
        assetRepository.deleteAll();

        seedStarterHolding("AAPL", "Apple Inc.", 10D);
        seedStarterHolding("TSLA", "Tesla Inc.", 5D);
        seedStarterHolding("AMZN", "Amazon.com Inc.", 4D);
    }

    @Transactional
    public void resetUnsupportedOrEmptyData() {
        boolean hasTransactions = assetTransactionRepository.count() > 0;
        boolean hasUnsupportedAssets = assetRepository.findAll().stream()
                .map(Asset::getTicker)
                .anyMatch(ticker -> ticker != null && !SUPPORTED_TICKERS.contains(ticker));
        boolean hasUnsupportedTransactions = assetTransactionRepository.findAll().stream()
                .map(AssetTransaction::getTicker)
                .anyMatch(ticker -> ticker != null && !SUPPORTED_TICKERS.contains(ticker));

        if (!hasTransactions || hasUnsupportedAssets || hasUnsupportedTransactions) {
            resetToStarterData();
        }
    }

    private void seedStarterHolding(String ticker, String name, double quantity) {
        try {
            List<MarketHistoryPointDTO> history = marketDataService.getPriceHistory(ticker);
            if (history.isEmpty()) {
                return;
            }

            MarketHistoryPointDTO startPoint = history.stream()
                    .filter(point -> point.getTimestampValue() != null)
                    .filter(point -> {
                        LocalDate date = new Date(point.getTimestampValue()).toInstant()
                                .atZone(ZoneId.of("Asia/Shanghai"))
                                .toLocalDate();
                        return !date.isBefore(START_DATE);
                    })
                    .findFirst()
                    .orElse(history.get(0));

            MarketHistoryPointDTO latestPoint = history.get(history.size() - 1);
            double currentPrice;
            try {
                currentPrice = marketDataService.getMarketPrice(ticker);
            } catch (Exception ignored) {
                currentPrice = latestPoint.getClose() != null ? latestPoint.getClose().doubleValue() : 0D;
            }

            AssetTransactionDTO transactionDTO = new AssetTransactionDTO();
            transactionDTO.setTicker(ticker);
            transactionDTO.setAssetName(name);
            transactionDTO.setAssetType("STOCK");
            transactionDTO.setTransactionType("BUY");
            transactionDTO.setQuantity(quantity);
            transactionDTO.setPrice(startPoint.getClose() != null ? startPoint.getClose().doubleValue() : 0D);
            transactionDTO.setCurrentPrice(currentPrice);
            transactionDTO.setTransactionDate(START_DATE_TIME);
            transactionDTO.setNotes("Starter holding reset on 2026-03-27 09:30");
            assetTransactionService.createTransaction(transactionDTO);
        } catch (ResponseStatusException ignored) {
        }
    }
}
