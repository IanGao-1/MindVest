package com.reserio.financialmanagement.controller;

import com.reserio.financialmanagement.dto.HoldingValueHistoryDTO;
import com.reserio.financialmanagement.dto.MarketHistoryPointDTO;
import com.reserio.financialmanagement.dto.ValueHistoryPointDTO;
import com.reserio.financialmanagement.dto.YahooQuoteDTO;
import com.reserio.financialmanagement.service.MarketDataService;
import com.reserio.financialmanagement.service.PortfolioAnalyticsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/market-data")
@Tag(name = "Market Data", description = "Market data management API")
public class MarketDataController {

    @Autowired
    private MarketDataService marketDataService;

    @Autowired
    private PortfolioAnalyticsService portfolioAnalyticsService;

    @Operation(summary = "Get market price for a ticker")
    @GetMapping("/{ticker}")
    public ResponseEntity<Double> getMarketPrice(@PathVariable String ticker) {
        double price = marketDataService.getMarketPrice(ticker);
        return ResponseEntity.ok(price);
    }

    @Operation(summary = "Get quote details from Yahoo Finance by ticker")
    @GetMapping("/yahoo/{ticker}")
    public ResponseEntity<YahooQuoteDTO> getYahooQuote(@PathVariable String ticker) {
        YahooQuoteDTO quote = marketDataService.getYahooQuote(ticker);
        return ResponseEntity.ok(quote);
    }

    @Operation(summary = "Get historical market data by ticker")
    @GetMapping("/history/{ticker}")
    public ResponseEntity<java.util.List<MarketHistoryPointDTO>> getPriceHistory(@PathVariable String ticker) {
        return ResponseEntity.ok(marketDataService.getPriceHistory(ticker));
    }

    @Operation(summary = "Get value history for each current holding")
    @GetMapping("/holdings-history")
    public ResponseEntity<java.util.List<HoldingValueHistoryDTO>> getHoldingValueHistory() {
        return ResponseEntity.ok(portfolioAnalyticsService.getHoldingValueHistories());
    }

    @Operation(summary = "Get overall portfolio value history")
    @GetMapping("/portfolio-history")
    public ResponseEntity<java.util.List<ValueHistoryPointDTO>> getPortfolioHistory() {
        return ResponseEntity.ok(portfolioAnalyticsService.getPortfolioValueHistory());
    }

    @Operation(summary = "Update all asset prices")
    @PostMapping("/update")
    public ResponseEntity<Void> updateAssetPrices() {
        marketDataService.updateAssetPrices();
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Update asset price by ID")
    @PostMapping("/update/{assetId}")
    public ResponseEntity<Void> updateAssetPrice(@PathVariable Long assetId) {
        marketDataService.updateAssetPrice(assetId);
        return ResponseEntity.noContent().build();
    }
}
