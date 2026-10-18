package com.MySelfAlgoWithLoan.prod.calc.model;

public record PositionSizeResult(
        boolean accepted,
        double qty,
        double futuresSellVwap,
        double spotBuyVwap,
        String rejectReason
) {
    public static PositionSizeResult accepted(double qty, double futuresSellVwap, double spotBuyVwap) {
        return new PositionSizeResult(true, qty, futuresSellVwap, spotBuyVwap, null);
    }

    public static PositionSizeResult rejected(String reason) {
        return new PositionSizeResult(false, 0, 0, 0, reason);
    }
}