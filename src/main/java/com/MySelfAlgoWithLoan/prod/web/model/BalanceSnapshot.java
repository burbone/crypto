package com.MySelfAlgoWithLoan.prod.web.model;

public record BalanceSnapshot(
        double bybitNetWorthUsdt,
        double kucoinNetWorthUsdt,
        double totalNetWorthUsdt,
        double totalChangePercent
) {
}