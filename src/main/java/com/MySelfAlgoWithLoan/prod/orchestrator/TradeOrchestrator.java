package com.MySelfAlgoWithLoan.prod.orchestrator;

import com.MySelfAlgoWithLoan.prod.config.Properties;
import com.MySelfAlgoWithLoan.prod.domain.MarketTick;
import com.MySelfAlgoWithLoan.prod.marketdata.PriceStore;
import com.MySelfAlgoWithLoan.prod.strategy.TradingStrategy;
import com.MySelfAlgoWithLoan.prod.util.SymbolFormat;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class TradeOrchestrator {

    private static final String BYBIT = "BYBIT";
    private static final String BYBIT_FUTURES = "BYBIT_FUTURES";
    private static final String KUCOIN = "KUCOIN";
    private static final String KUCOIN_FUTURES = "KUCOIN_FUTURES";
    private static final long MAX_TICK_AGE_MS = 10_000;

    private final PriceStore priceStore;
    private final Properties properties;
    private final TradingStrategy strategy;

    @Scheduled(fixedDelay = 1000)
    public void tick() {
        List<String> pairs = properties.getTrackedPairs();
        if (pairs == null || pairs.isEmpty()) {
            return;
        }

        for (String symbol : pairs) {
            MarketTick bybitSpot = priceStore.get(BYBIT, symbol);
            MarketTick bybitFut = priceStore.get(BYBIT_FUTURES, symbol);
            MarketTick kucoinSpot = priceStore.get(KUCOIN, SymbolFormat.toKucoinSymbol(symbol));
            MarketTick kucoinFut = priceStore.get(KUCOIN_FUTURES, toKucoinFuturesSymbol(symbol));

            if (isFresh(bybitSpot) && isFresh(bybitFut)
                    && isFresh(kucoinSpot) && isFresh(kucoinFut)) {
                strategy.onTick(symbol, bybitSpot, bybitFut, kucoinSpot, kucoinFut);
            }
        }
    }

    public String manualEntryDetailed(String symbol, String side) {
        return "Manual entry disabled";
    }

    private boolean isFresh(MarketTick tick) {
        return tick != null && System.currentTimeMillis() - tick.timestamp() < MAX_TICK_AGE_MS;
    }

    private String toKucoinFuturesSymbol(String bybitSymbol) {
        String base = SymbolFormat.coinFromSymbol(bybitSymbol);
        if ("BTC".equals(base)) {
            base = "XBT";
        }
        return base + "USDTM";
    }
}