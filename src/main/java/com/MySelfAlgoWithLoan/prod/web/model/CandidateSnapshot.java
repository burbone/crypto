package com.MySelfAlgoWithLoan.prod.web.model;

public record CandidateSnapshot(
        String symbol,
        String side,
        double futuresPrice,
        double spotPrice,
        double currentSpreadPercent,
        double entryTriggerSpread
) {
}