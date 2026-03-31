package com.reserio.financialmanagement.context;

import java.math.BigDecimal;
import java.util.concurrent.atomic.AtomicReference;

public final class AccountBalanceContext {
    private static final BigDecimal INITIAL_BALANCE = BigDecimal.valueOf(10000);
    private static final AtomicReference<BigDecimal> GLOBAL_BALANCE =
            new AtomicReference<>(INITIAL_BALANCE);
    private static final ThreadLocal<BigDecimal> BALANCE_HOLDER =
            ThreadLocal.withInitial(GLOBAL_BALANCE::get);

    private AccountBalanceContext() {
    }

    public static BigDecimal getBalance() {
        return BALANCE_HOLDER.get();
    }

    public static void setBalance(BigDecimal balance) {
        GLOBAL_BALANCE.set(balance);
        BALANCE_HOLDER.set(balance);
    }

    public static void syncFromGlobal() {
        BALANCE_HOLDER.set(GLOBAL_BALANCE.get());
    }

    public static void clear() {
        BALANCE_HOLDER.remove();
    }

    public static void reset() {
        setBalance(INITIAL_BALANCE);
    }
}
