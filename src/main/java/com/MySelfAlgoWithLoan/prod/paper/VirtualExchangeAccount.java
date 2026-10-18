package com.MySelfAlgoWithLoan.prod.paper;

import java.math.BigDecimal;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class VirtualExchangeAccount {

    private final ConcurrentHashMap<String, BigDecimal> balances = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, BigDecimal> debt = new ConcurrentHashMap<>();

    public synchronized BigDecimal getBalance(String currency) {
        return balances.getOrDefault(currency, BigDecimal.ZERO);
    }

    public synchronized BigDecimal getDebt(String coin) {
        return debt.getOrDefault(coin, BigDecimal.ZERO);
    }

    public synchronized void credit(String currency, BigDecimal amount) {
        requirePositive(amount);
        balances.merge(currency, amount, BigDecimal::add);
    }

    public synchronized void debit(String currency, BigDecimal amount) {
        requirePositive(amount);
        BigDecimal current = getBalance(currency);
        if (current.compareTo(amount) < 0) {
            throw new InsufficientVirtualBalanceException(currency, amount, current);
        }
        balances.put(currency, current.subtract(amount));
    }

    public synchronized void borrow(String coin, BigDecimal amount) {
        requirePositive(amount);
        balances.merge(coin, amount, BigDecimal::add);
        debt.merge(coin, amount, BigDecimal::add);
    }

    public synchronized void repay(String coin, BigDecimal amount) {
        requirePositive(amount);
        debit(coin, amount);
        BigDecimal currentDebt = getDebt(coin);
        debt.put(coin, currentDebt.subtract(amount).max(BigDecimal.ZERO));
    }

    public Map<String, BigDecimal> allBalances() {
        return Map.copyOf(balances);
    }

    public Map<String, BigDecimal> allDebts() {
        return Map.copyOf(debt);
    }

    private void requirePositive(BigDecimal amount) {
        if (amount == null || amount.signum() <= 0) {
            throw new IllegalArgumentException("Сумма должна быть положительной: " + amount);
        }
    }
}