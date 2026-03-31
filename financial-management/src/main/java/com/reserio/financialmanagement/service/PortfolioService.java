package com.reserio.financialmanagement.service;

import com.reserio.financialmanagement.dto.AssetDTO;
import com.reserio.financialmanagement.dto.PortfolioDTO;
import com.reserio.financialmanagement.model.Portfolio;
import com.reserio.financialmanagement.model.PortfolioAsset;
import com.reserio.financialmanagement.repository.AssetRepository;
import com.reserio.financialmanagement.repository.PortfolioAssetRepository;
import com.reserio.financialmanagement.repository.PortfolioRepository;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class PortfolioService {

    @Autowired
    private PortfolioRepository portfolioRepository;

    @Autowired
    private PortfolioAssetRepository portfolioAssetRepository;

    @Autowired
    private AssetRepository assetRepository;

    @Autowired
    private AssetService assetService;

    public List<PortfolioDTO> getAllPortfolios() {
        return portfolioRepository.findAll().stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    public PortfolioDTO getPortfolioById(Long id) {
        Portfolio portfolio = portfolioRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Portfolio not found"));
        return convertToDTO(portfolio);
    }

    public PortfolioDTO createPortfolio(PortfolioDTO portfolioDTO) {
        Portfolio portfolio = convertToEntity(portfolioDTO);
        portfolio.setCreatedAt(new Date());
        Portfolio savedPortfolio = portfolioRepository.save(portfolio);
        return convertToDTO(savedPortfolio);
    }

    public PortfolioDTO updatePortfolio(Long id, PortfolioDTO portfolioDTO) {
        Portfolio portfolio = portfolioRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Portfolio not found"));
        BeanUtils.copyProperties(portfolioDTO, portfolio, "id", "createdAt");
        Portfolio updatedPortfolio = portfolioRepository.save(portfolio);
        return convertToDTO(updatedPortfolio);
    }

    public void deletePortfolio(Long id) {
        // First delete portfolio-asset associations
        List<PortfolioAsset> portfolioAssets = portfolioAssetRepository.findByPortfolioId(id);
        portfolioAssetRepository.deleteAll(portfolioAssets);
        // Then delete the portfolio
        portfolioRepository.deleteById(id);
    }

    public void addAssetToPortfolio(Long portfolioId, Long assetId) {
        PortfolioAsset portfolioAsset = new PortfolioAsset();
        portfolioAsset.setPortfolioId(portfolioId);
        portfolioAsset.setAssetId(assetId);
        portfolioAssetRepository.save(portfolioAsset);
    }

    public void removeAssetFromPortfolio(Long portfolioId, Long assetId) {
        List<PortfolioAsset> portfolioAssets = portfolioAssetRepository.findByPortfolioId(portfolioId);
        PortfolioAsset portfolioAsset = portfolioAssets.stream()
                .filter(pa -> pa.getAssetId().equals(assetId))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Asset not found in portfolio"));
        portfolioAssetRepository.delete(portfolioAsset);
    }

    private PortfolioDTO convertToDTO(Portfolio portfolio) {
        PortfolioDTO portfolioDTO = new PortfolioDTO();
        BeanUtils.copyProperties(portfolio, portfolioDTO);
        
        // Get assets in the portfolio
        List<PortfolioAsset> portfolioAssets = portfolioAssetRepository.findByPortfolioId(portfolio.getId());
        List<AssetDTO> assets = portfolioAssets.stream()
                .map(pa -> assetService.getAssetById(pa.getAssetId()))
                .collect(Collectors.toList());
        portfolioDTO.setAssets(assets);
        portfolioDTO.setAssetCount(assets.size());
        
        // Calculate total value
        double totalValue = assets.stream()
                .mapToDouble(asset -> asset.getQuantity() * asset.getCurrentPrice())
                .sum();
        portfolioDTO.setTotalValue(totalValue);
        
        return portfolioDTO;
    }

    private Portfolio convertToEntity(PortfolioDTO portfolioDTO) {
        Portfolio portfolio = new Portfolio();
        BeanUtils.copyProperties(portfolioDTO, portfolio);
        return portfolio;
    }
}