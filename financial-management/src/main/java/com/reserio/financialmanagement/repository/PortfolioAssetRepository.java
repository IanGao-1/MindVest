package com.reserio.financialmanagement.repository;

import com.reserio.financialmanagement.model.PortfolioAsset;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PortfolioAssetRepository extends JpaRepository<PortfolioAsset, Long> {
    List<PortfolioAsset> findByPortfolioId(Long portfolioId);
    List<PortfolioAsset> findByAssetId(Long assetId);
}