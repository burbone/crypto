package com.MySelfAlgoWithLoan.prod.paper;

import java.math.BigDecimal;

public class InsufficientVirtualBalanceException extends RuntimeException {

    public InsufficientVirtualBalanceException(String currency, BigDecimal requested, BigDecimal available) {
        super("Недостаточно " + currency + " на виртуальном балансе: нужно " + requested.toPlainString()
                + ", доступно " + available.toPlainString());
    }
}