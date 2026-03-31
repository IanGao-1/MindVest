package com.reserio.financialmanagement.controller;

import com.reserio.financialmanagement.service.AccountBalanceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.Map;

@RestController
@RequestMapping("/api/account")
@Tag(name = "Account", description = "Account balance API")
public class AccountController {

    @Autowired
    private AccountBalanceService accountBalanceService;

    @Operation(summary = "Get account cash balance")
    @GetMapping("/balance")
    public ResponseEntity<Map<String, BigDecimal>> getBalance() {
        return ResponseEntity.ok(Collections.singletonMap("balance", accountBalanceService.getBalance()));
    }
}
