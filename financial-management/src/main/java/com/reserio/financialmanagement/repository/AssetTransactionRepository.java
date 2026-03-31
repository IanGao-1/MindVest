package com.reserio.financialmanagement.repository;

import com.reserio.financialmanagement.model.AssetTransaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AssetTransactionRepository extends JpaRepository<AssetTransaction, Long> {
    List<AssetTransaction> findAllByOrderByTransactionDateDescIdDesc();
    List<AssetTransaction> findByTickerOrderByTransactionDateDescIdDesc(String ticker);
}
