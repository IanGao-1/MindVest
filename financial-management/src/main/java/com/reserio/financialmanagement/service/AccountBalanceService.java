package com.reserio.financialmanagement.service;

import com.reserio.financialmanagement.context.AccountBalanceContext;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
public class AccountBalanceService {

    public BigDecimal getBalance() {
        return AccountBalanceContext.getBalance();
    }

    public void ensureSufficientBalance(BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            return;
        }

        if (getBalance().compareTo(amount) < 0) {
            throw new IllegalArgumentException("Insufficient balance");
        }
    }

    public BigDecimal debit(BigDecimal amount) {
        validateAmount(amount);
        ensureSufficientBalance(amount);
        BigDecimal updatedBalance = getBalance().subtract(amount);
        AccountBalanceContext.setBalance(updatedBalance);
        return updatedBalance;
    }

    public BigDecimal credit(BigDecimal amount) {
        validateAmount(amount);
        BigDecimal updatedBalance = getBalance().add(amount);
        AccountBalanceContext.setBalance(updatedBalance);
        return updatedBalance;
    }

    private void validateAmount(BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Amount must be greater than or equal to 0");
        }
    }
}
