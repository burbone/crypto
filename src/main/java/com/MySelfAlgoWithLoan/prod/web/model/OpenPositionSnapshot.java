package com.MySelfAlgoWithLoan.prod.web.model;

public record OpenPositionSnapshot(
        String symbol,
        String side,
        double currentProfitPercent,
        double currentProfitUsdt,
        double futuresPrice,
        double spotPrice,
        long entryTimestampMillis
) {
}