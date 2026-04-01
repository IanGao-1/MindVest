package com.reserio.financialmanagement.repository;

import com.reserio.financialmanagement.model.Asset;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AssetRepository extends JpaRepository<Asset, Long> {
    List<Asset> findByType(String type);
    List<Asset> findAllByTickerOrderByIdAsc(String ticker);
}
