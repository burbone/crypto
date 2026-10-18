package com.MySelfAlgoWithLoan.prod.paper;

import com.MySelfAlgoWithLoan.prod.calc.ProfitCalculator;
import com.MySelfAlgoWithLoan.prod.calc.VwapCalculator;
import com.MySelfAlgoWithLoan.prod.config.Properties;
import com.MySelfAlgoWithLoan.prod.domain.MarketTick;
import com.MySelfAlgoWithLoan.prod.marketdata.PriceStore;
import com.MySelfAlgoWithLoan.prod.calc.model.EntrySide;
import com.MySelfAlgoWithLoan.prod.util.SymbolFormat;
import com.MySelfAlgoWithLoan.prod.web.model.OpenPositionSnapshot;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
@RequiredArgsConstructor
public class OpenPositionValuationService {

    private static final String BYBIT = "BYBIT";
    private static final String KUCOIN = "KUCOIN";
    private static final String BYBIT_FUTURES = "BYBIT_FUTURES";
    private static final String KUCOIN_FUTURES = "KUCOIN_FUTURES";

    private final PositionRegistry positionRegistry;
    private final PriceStore priceStore;
    private final VwapCalculator vwapCalculator;
    private final ProfitCalculator profitCalculator;
    private final Properties properties;

    public List<OpenPositionSnapshot> snapshot() {
        List<OpenPositionSnapshot> result = new ArrayList<>();
        for (OpenPosition position : positionRegistry.allOpenPositions()) {
            OpenPositionSnapshot snap = valuate(position);
            if (snap != null) {
                result.add(snap);
            }
        }
        return result;
    }

    private OpenPositionSnapshot valuate(OpenPosition position) {
        String symbol = position.symbol();
        EntrySide side = position.side();

        MarketTick futuresTick = getFuturesTick(side, symbol);
        MarketTick spotTick = getSpotTick(side, symbol);
        if (futuresTick == null || spotTick == null) {
            return null;
        }

        try {
            double exitFuturesBuyVwap = vwapCalculator.vwap(futuresTick.asks(), position.qty());
            double exitSpotSellVwap = vwapCalculator.vwap(spotTick.bids(), position.qty());

            double feeFutures = feeForFutures(side);
            double feeSpot = feeForSpot(side);

            double profitUsdt = profitCalculator.calculate(
                    position.qty(),
                    position.futuresEntryVwap(),
                    position.spotEntryVwap(),
                    exitFuturesBuyVwap,
                    exitSpotSellVwap,
                    feeFutures,
                    feeSpot
            );

            double nominalUsdt = position.qty() * position.futuresEntryVwap();
            double profitPercent = nominalUsdt == 0 ? 0 : profitUsdt / nominalUsdt * 100;

            double futuresAsk = futuresTick.ask();
            double spotBid = spotTick.bid();

            return new OpenPositionSnapshot(
                    symbol,
                    side.name(),
                    profitPercent,
                    profitUsdt,
                    futuresAsk,
                    spotBid,
                    position.entryTimestampMillis()
            );
        } catch (Exception e) {
            return null;
        }
    }

    private MarketTick getFuturesTick(EntrySide side, String symbol) {
        return switch (side) {
            case BYBIT_FUTURES_SHORT__KUCOIN_SPOT_LONG -> priceStore.get(BYBIT_FUTURES, symbol);
            case KUCOIN_FUTURES_SHORT__BYBIT_SPOT_LONG -> priceStore.get(KUCOIN_FUTURES, symbol);
        };
    }

    private MarketTick getSpotTick(EntrySide side, String symbol) {
        return switch (side) {
            case BYBIT_FUTURES_SHORT__KUCOIN_SPOT_LONG -> priceStore.get(KUCOIN, SymbolFormat.toKucoinSymbol(symbol));
            case KUCOIN_FUTURES_SHORT__BYBIT_SPOT_LONG -> priceStore.get(BYBIT, symbol);
        };
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