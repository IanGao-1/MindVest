package com.reserio.financialmanagement.controller;

import com.reserio.financialmanagement.dto.AssetTransactionDTO;
import com.reserio.financialmanagement.service.AssetTransactionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/transactions")
@Tag(name = "Asset Transaction", description = "Asset transaction API")
public class AssetTransactionController {

    @Autowired
    private AssetTransactionService assetTransactionService;

    @Operation(summary = "Get all transactions")
    @GetMapping
    public ResponseEntity<List<AssetTransactionDTO>> getAllTransactions() {
        return ResponseEntity.ok(assetTransactionService.getAllTransactions());
    }

    @Operation(summary = "Create transaction and update asset snapshot")
    @PostMapping
    public ResponseEntity<AssetTransactionDTO> createTransaction(@RequestBody AssetTransactionDTO transactionDTO) {
        AssetTransactionDTO createdTransaction = assetTransactionService.createTransaction(transactionDTO);
        return ResponseEntity.status(HttpStatus.CREATED).body(createdTransaction);
    }
}
