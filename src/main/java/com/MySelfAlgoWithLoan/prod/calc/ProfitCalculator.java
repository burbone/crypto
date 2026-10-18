package com.MySelfAlgoWithLoan.prod.calc;

import org.springframework.stereotype.Component;

@Component
public class ProfitCalculator {

    public double calculate(double qty,
                            double entryFuturesSellVwap,
                            double entrySpotBuyVwap,
                            double exitFuturesBuyVwap,
                            double exitSpotSellVwap,
                            double feeFutures,
                            double feeSpot) {
        double sellFuturesLeg = qty * entryFuturesSellVwap * (1 - feeFutures);
        double buySpotLeg = qty * entrySpotBuyVwap * (1 + feeSpot);
        double buyFuturesLeg = qty * exitFuturesBuyVwap * (1 + feeFutures);
        double sellSpotLeg = qty * exitSpotSellVwap * (1 - feeSpot);

        return sellFuturesLeg - buySpotLeg - buyFuturesLeg + sellSpotLeg;
    }
}