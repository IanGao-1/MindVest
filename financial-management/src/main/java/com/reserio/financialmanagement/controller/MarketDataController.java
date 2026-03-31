package com.reserio.financialmanagement.controller;

import com.reserio.financialmanagement.dto.YahooQuoteDTO;
import com.reserio.financialmanagement.service.MarketDataService;
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
