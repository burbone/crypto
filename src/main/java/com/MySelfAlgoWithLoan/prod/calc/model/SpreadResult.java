package com.MySelfAlgoWithLoan.prod.calc.model;

public record SpreadResult(
        double grossSpread,
        double netSpread,
        double slipExitBuffer
) {
}