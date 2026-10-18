package com.MySelfAlgoWithLoan.prod.database;

import java.time.Instant;

public record SpreadDataRecord(
        String symbol,
        String side,
        Instant timestamp,
        double bybitFuturesBid,
        double bybitFuturesAsk,
        String bybitFuturesBidsJson,
        String bybitFuturesAsksJson,
        double kucoinSpotBid,
        double kucoinSpotAsk,
        String kucoinSpotBidsJson,
        String kucoinSpotAsksJson,
        double netSpreadPercent
) {
}