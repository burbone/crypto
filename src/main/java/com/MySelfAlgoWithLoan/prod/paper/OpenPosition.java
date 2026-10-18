package com.MySelfAlgoWithLoan.prod.paper;

import com.MySelfAlgoWithLoan.prod.calc.model.EntrySide;

public record OpenPosition(
        String symbol,
        EntrySide side,
        double qty,
        double futuresEntryVwap,
        double spotEntryVwap,
        double liquidationPrice,
        long entryTimestampMillis,
        boolean profitTargetReached,
        double maxProfitPercent
) {
}