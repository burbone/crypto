package com.MySelfAlgoWithLoan.prod.calc.model;

public record ExitDecision(
        boolean shouldExit,
        boolean forced,
        String reason
) {
    public static ExitDecision exit(boolean forced, String reason) {
        return new ExitDecision(true, forced, reason);
    }

    public static ExitDecision hold() {
        return new ExitDecision(false, false, "hold");
    }
}