package com.MySelfAlgoWithLoan.prod.database;

import com.MySelfAlgoWithLoan.prod.calc.model.EntrySide;
import com.MySelfAlgoWithLoan.prod.domain.MarketTick;
import com.google.gson.Gson;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

@Component
@RequiredArgsConstructor
public class DatabaseSpreadLogger {

    private final SpreadDataRepository repository;
    private final Gson gson = new Gson();
    private final Queue<SpreadDataRecord> queue = new ConcurrentLinkedQueue<>();

    public void enqueue(String symbol, EntrySide side, MarketTick futuresTick,
                        MarketTick spotTick, double netSpreadPercent) {
        SpreadDataRecord record = new SpreadDataRecord(
                symbol,
                side.name(),
                Instant.now(),
                futuresTick.bid(),
                futuresTick.ask(),
                toJson(futuresTick.bids()),
                toJson(futuresTick.asks()),
                spotTick.bid(),
                spotTick.ask(),
                toJson(spotTick.bids()),
                toJson(spotTick.asks()),
                netSpreadPercent
        );
        queue.offer(record);
    }

    @Scheduled(fixedDelay = 2000)
    public void flush() {
        if (queue.isEmpty()) {
            return;
        }
        List<SpreadDataRecord> batch = new ArrayList<>(queue.size());
        SpreadDataRecord record;
        while ((record = queue.poll()) != null) {
            batch.add(record);
        }
        try {
            repository.saveAll(batch);
        } catch (Exception e) {
            System.err.println("Database write failed: " + e.getMessage());
        }
    }

    private String toJson(List<MarketTick.OrderBookLevel> levels) {
        return gson.toJson(levels);
    }
}