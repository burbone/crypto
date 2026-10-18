package com.MySelfAlgoWithLoan.prod.scheduler;

import com.MySelfAlgoWithLoan.prod.api.ws.WsClientBase;
import com.MySelfAlgoWithLoan.prod.config.Properties;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

@Component
@RequiredArgsConstructor
public class MarketDataRestartScheduler {

    private final List<WsClientBase> allClients;
    private final Properties properties;

    private ScheduledExecutorService scheduler;
    private ScheduledFuture<?> scheduledTask;

    @PostConstruct
    public void start() {
        long periodMillis = Long.parseLong(properties.getMarketDataRestartMillis());
        scheduler = Executors.newSingleThreadScheduledExecutor();
        scheduledTask = scheduler.scheduleAtFixedRate(
                this::restartAll,
                periodMillis,
                periodMillis,
                TimeUnit.MILLISECONDS
        );
        System.out.println("Плановое пересоздание WebSocket каждые " + periodMillis + " мс");
    }

    public void restartAll() {
        System.out.println("Полное пересоздание WebSocket-соединений...");
        for (WsClientBase client : allClients) {
            client.shutdown();
        }
        for (WsClientBase client : allClients) {
            client.connect(client.getSymbols());
        }
    }

    @PreDestroy
    public void shutdown() {
        if (scheduledTask != null) {
            scheduledTask.cancel(false);
        }
        if (scheduler != null) {
            scheduler.shutdown();
        }
    }
}