package com.MySelfAlgoWithLoan.prod.calc.model;

public record EntryDecision(
        boolean shouldEnter,
        String symbol,
        EntrySide side,
        double qty,
        SpreadResult spread,
        PositionSizeResult sizeResult,
        String reason
) {
    public static EntryDecision accept(String symbol, EntrySide side, PositionSizeResult sizeResult, SpreadResult spread) {
        return new EntryDecision(true, symbol, side, sizeResult.qty(), spread, sizeResult, "accepted");
    }

    public static EntryDecision reject(String symbol, EntrySide side, String reason) {
        return new EntryDecision(false, symbol, side, 0, null, null, reason);
    }
}