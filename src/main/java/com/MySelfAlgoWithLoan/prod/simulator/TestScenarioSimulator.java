package com.MySelfAlgoWithLoan.prod.simulator;

import com.MySelfAlgoWithLoan.prod.config.Properties;
import com.MySelfAlgoWithLoan.prod.domain.MarketTick;
import com.MySelfAlgoWithLoan.prod.marketdata.PriceStore;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
@RequiredArgsConstructor
public class TestScenarioSimulator {

    private static final String BYBIT_FUTURES = "BYBIT_FUTURES";
    private static final String KUCOIN = "KUCOIN";
    private static final double BASE_PRICE = 100.0;
    private static final double LEVEL_SIZE = 500.0;
    private static final int LEVELS_COUNT = 5;
    private static final double TICK_SIZE = 0.001;

    private final PriceStore priceStore;
    private final Properties properties;

    private final List<String> testSymbols = new ArrayList<>();
    private boolean initialized = false;

    @Scheduled(fixedDelay = 200)
    public void generate() {
        if (!properties.isTestModeEnabled()) {
            return;
        }

        if (!initialized) {
            if (properties.getTestPairs() != null) {
                testSymbols.addAll(properties.getTestPairs());
            }
            initialized = true;
        }

        for (String symbol : testSymbols) {
            generateTick(symbol);
        }
    }

    private void generateTick(String symbol) {
        double mid = BASE_PRICE;
        long ts = System.currentTimeMillis();

        List<MarketTick.OrderBookLevel> bybitBids = buildLevels(mid, true);
        List<MarketTick.OrderBookLevel> bybitAsks = buildLevels(mid, false);
        priceStore.updateOrderBook(BYBIT_FUTURES, symbol, bybitBids, bybitAsks, ts);
        priceStore.updatePrice(BYBIT_FUTURES, symbol, mid, mid, mid, ts);

        String kucoinSymbol = symbol.contains("USDT") ? symbol.replace("USDT", "-USDT") : symbol + "-USDT";
        List<MarketTick.OrderBookLevel> kucoinBids = buildLevels(mid, true);
        List<MarketTick.OrderBookLevel> kucoinAsks = buildLevels(mid, false);
        priceStore.updateOrderBook(KUCOIN, kucoinSymbol, kucoinBids, kucoinAsks, ts);
        priceStore.updatePrice(KUCOIN, kucoinSymbol, mid, mid, mid, ts);
    }

    private List<MarketTick.OrderBookLevel> buildLevels(double topPrice, boolean isBid) {
        List<MarketTick.OrderBookLevel> levels = new ArrayList<>();
        double step = isBid ? -TICK_SIZE : TICK_SIZE;
        for (int i = 0; i < LEVELS_COUNT; i++) {
            double price = topPrice + step * i;
            levels.add(new MarketTick.OrderBookLevel(price, LEVEL_SIZE));
        }
        return levels;
    }
}