package com.MySelfAlgoWithLoan.prod.calc;

import com.MySelfAlgoWithLoan.prod.calc.model.EntrySide;
import com.MySelfAlgoWithLoan.prod.calc.model.PositionSizeResult;
import com.MySelfAlgoWithLoan.prod.config.Properties;
import com.MySelfAlgoWithLoan.prod.domain.MarketTick;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class PositionSizeCalculator {

    private static final int MAX_ITERATIONS = 50;
    private static final double ITERATION_TOLERANCE = 0.001;

    private final VwapCalculator vwapCalculator;
    private final Properties properties;

    public PositionSizeResult calculate(EntrySide side,
                                        MarketTick futuresTick,
                                        MarketTick spotTick,
                                        double futuresBalanceUsdt,
                                        double spotBalanceUsdt) {
        double feeFutures = feeForFutures(side);
        double feeSpot = feeForSpot(side);
        double maxSlippagePercent = properties.getMaxSlippagePercent();

        double availableFuturesQty = vwapCalculator.getAvailableQty(futuresTick.bids());
        double availableSpotQty = vwapCalculator.getAvailableQty(spotTick.asks());
        double maxAvailableQty = Math.min(availableFuturesQty, availableSpotQty);

        double qDepth = Math.min(
                vwapCalculator.maxDepthQty(futuresTick.bids(), futuresTick.bid(), maxSlippagePercent),
                vwapCalculator.maxDepthQty(spotTick.asks(), spotTick.ask(), maxSlippagePercent)
        );

        double qLimit = Math.min(qDepth, maxAvailableQty);
        if (qLimit <= 0) {
            return PositionSizeResult.rejected("no available order book volume");
        }

        double q = Math.min(
                futuresBalanceUsdt / futuresTick.bid(),
                spotBalanceUsdt / (spotTick.ask() * (1 + feeSpot))
        );
        q = Math.min(q, qLimit);

        if (q <= 0) {
            return PositionSizeResult.rejected("initial position size <= 0");
        }

        for (int i = 0; i < MAX_ITERATIONS; i++) {
            if (q > qLimit) {
                q = qLimit;
            }

            double futuresSellVwap = vwapCalculator.vwap(futuresTick.bids(), q);
            double spotBuyVwap = vwapCalculator.vwap(spotTick.asks(), q);

            double nextQ = Math.min(
                    futuresBalanceUsdt / futuresSellVwap,
                    spotBalanceUsdt / (spotBuyVwap * (1 + feeSpot))
            );
            nextQ = Math.min(nextQ, qLimit);

            double delta = Math.abs(nextQ - q) / q;
            q = nextQ;
            if (delta < ITERATION_TOLERANCE) {
                break;
            }
        }

        if (q > qLimit) {
            q = qLimit;
        }
        if (q <= 0) {
            return PositionSizeResult.rejected("position size converged to <= 0");
        }

        double finalFuturesSellVwap = vwapCalculator.vwap(futuresTick.bids(), q);
        double finalSpotBuyVwap = vwapCalculator.vwap(spotTick.asks(), q);

        return PositionSizeResult.accepted(q, finalFuturesSellVwap, finalSpotBuyVwap);
    }

    private double feeForFutures(EntrySide side) {
        return switch (side) {
            case BYBIT_FUTURES_SHORT__KUCOIN_SPOT_LONG -> properties.getBybitFuturesTakerFeePercent() / 100.0;
            case KUCOIN_FUTURES_SHORT__BYBIT_SPOT_LONG -> properties.getKucoinFuturesTakerFeePercent() / 100.0;
        };
    }

    private double feeForSpot(EntrySide side) {
        return switch (side) {
            case BYBIT_FUTURES_SHORT__KUCOIN_SPOT_LONG -> properties.getKucoinSpotTakerFeePercent() / 100.0;
            case KUCOIN_FUTURES_SHORT__BYBIT_SPOT_LONG -> properties.getBybitSpotTakerFeePercent() / 100.0;
        };
    }
}