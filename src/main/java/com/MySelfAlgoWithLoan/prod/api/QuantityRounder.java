package com.MySelfAlgoWithLoan.prod.api;

import java.math.BigDecimal;
import java.math.RoundingMode;

public final class QuantityRounder {

    private QuantityRounder() {
    }

    public static BigDecimal roundDown(BigDecimal rawQty, BigDecimal step) {
        if (step == null || step.signum() <= 0) {
            return rawQty;
        }
        BigDecimal steps = rawQty.divide(step, 0, RoundingMode.FLOOR);
        return steps.multiply(step).stripTrailingZeros();
    }

    public static String toOrderString(BigDecimal qty) {
        return qty.toPlainString();
    }
}