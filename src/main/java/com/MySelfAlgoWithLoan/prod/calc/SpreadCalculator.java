package com.MySelfAlgoWithLoan.prod.calc;

import com.MySelfAlgoWithLoan.prod.calc.model.EntrySide;
import com.MySelfAlgoWithLoan.prod.calc.model.SpreadResult;
import com.MySelfAlgoWithLoan.prod.config.Properties;
import com.MySelfAlgoWithLoan.prod.domain.MarketTick;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class SpreadCalculator {

    private final VwapCalculator vwapCalculator;
    private final Properties properties;

    public SpreadResult calculate(EntrySide side, MarketTick futuresTick, MarketTick spotTick,
                                  double qty, double futuresSellVwap, double spotBuyVwap) {
        double feeFutures = feeForFutures(side);
        double feeSpot = feeForSpot(side);

        double grossSpread = futuresSellVwap / spotBuyVwap - 1;

        double futuresBuyVwapAtClose = vwapCalculator.vwap(futuresTick.asks(), qty);
        double futuresBuyTop = futuresTick.ask();

        double spotSellVwapAtClose = vwapCalculator.vwap(spotTick.bids(), qty);
        double spotSellTop = spotTick.bid();

        double slipExitBuffer = (futuresBuyVwapAtClose / futuresBuyTop - 1)
                + (spotSellTop / spotSellVwapAtClose - 1);

        double netSpread = grossSpread - 2 * (feeFutures + feeSpot) - slipExitBuffer;

        return new SpreadResult(grossSpread, netSpread, slipExitBuffer);
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