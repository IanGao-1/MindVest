package com.reserio.financialmanagement.controller;

import com.reserio.financialmanagement.dto.PortfolioDTO;
import com.reserio.financialmanagement.service.PortfolioService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/portfolios")
@Tag(name = "Portfolio", description = "Portfolio management API")
public class PortfolioController {

    @Autowired
    private PortfolioService portfolioService;

    @Operation(summary = "Get all portfolios")
    @GetMapping
    public ResponseEntity<List<PortfolioDTO>> getAllPortfolios() {
        List<PortfolioDTO> portfolios = portfolioService.getAllPortfolios();
        return ResponseEntity.ok(portfolios);
    }

    @Operation(summary = "Get portfolio by ID")
    @GetMapping("/{id}")
    public ResponseEntity<PortfolioDTO> getPortfolioById(@PathVariable Long id) {
        PortfolioDTO portfolio = portfolioService.getPortfolioById(id);
        return ResponseEntity.ok(portfolio);
    }

    @Operation(summary = "Create portfolio")
    @PostMapping
    public ResponseEntity<PortfolioDTO> createPortfolio(@RequestBody PortfolioDTO portfolioDTO) {
        PortfolioDTO createdPortfolio = portfolioService.createPortfolio(portfolioDTO);
        return ResponseEntity.status(HttpStatus.CREATED).body(createdPortfolio);
    }

    @Operation(summary = "Update portfolio")
    @PutMapping("/{id}")
    public ResponseEntity<PortfolioDTO> updatePortfolio(@PathVariable Long id, @RequestBody PortfolioDTO portfolioDTO) {
        PortfolioDTO updatedPortfolio = portfolioService.updatePortfolio(id, portfolioDTO);
        return ResponseEntity.ok(updatedPortfolio);
    }

    @Operation(summary = "Delete portfolio")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletePortfolio(@PathVariable Long id) {
        portfolioService.deletePortfolio(id);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Add asset to portfolio")
    @PostMapping("/{portfolioId}/assets/{assetId}")
    public ResponseEntity<Void> addAssetToPortfolio(@PathVariable Long portfolioId, @PathVariable Long assetId) {
        portfolioService.addAssetToPortfolio(portfolioId, assetId);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @Operation(summary = "Remove asset from portfolio")
    @DeleteMapping("/{portfolioId}/assets/{assetId}")
    public ResponseEntity<Void> removeAssetFromPortfolio(@PathVariable Long portfolioId, @PathVariable Long assetId) {
        portfolioService.removeAssetFromPortfolio(portfolioId, assetId);
        return ResponseEntity.noContent().build();
    }
}