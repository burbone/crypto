package com.MySelfAlgoWithLoan.prod.scheduler;

import com.MySelfAlgoWithLoan.prod.config.Properties;
import com.MySelfAlgoWithLoan.prod.domain.MarketTick;
import com.MySelfAlgoWithLoan.prod.marketdata.PriceStore;
import com.MySelfAlgoWithLoan.prod.util.SymbolFormat;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
@RequiredArgsConstructor
public class PriceStoreDiagnostics {

    private static final long FRESH_THRESHOLD_MS = 30_000;

    private final PriceStore priceStore;
    private final Properties properties;

    @Scheduled(fixedRate = 300_000)
    public void printSummary() {
        long now = System.currentTimeMillis();
        long freshAfter = now - FRESH_THRESHOLD_MS;

        int total = properties.getTrackedPairs().size();
        int fullyFresh = 0;
        List<String> problematic = new ArrayList<>();

        for (String pair : properties.getTrackedPairs()) {
            boolean bybitSpot = isFresh(priceStore.get("BYBIT", pair), freshAfter);
            boolean bybitFutures = isFresh(priceStore.get("BYBIT_FUTURES", pair), freshAfter);
            boolean kucoinSpot = isFresh(priceStore.get("KUCOIN", SymbolFormat.toKucoinSymbol(pair)), freshAfter);
            boolean kucoinFutures = isFresh(priceStore.get("KUCOIN_FUTURES", pair + "M"), freshAfter);

            if (bybitSpot && bybitFutures && kucoinSpot && kucoinFutures) {
                fullyFresh++;
            } else {
                problematic.add(pair);
            }
        }

        if (fullyFresh == total) {
            System.out.printf("Стабильно: %d/%d символов обрабатываются%n", fullyFresh, total);
        } else {
            System.out.printf("Внимание: %d/%d символов обрабатываются, проблемы: %s%n",
                    fullyFresh, total, problematic);
        }
    }

    private boolean isFresh(MarketTick tick, long freshAfter) {
        return tick != null && tick.timestamp() >= freshAfter;
    }
}