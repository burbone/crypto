package com.MySelfAlgoWithLoan.prod.marketdata;

import com.MySelfAlgoWithLoan.prod.domain.MarketTick;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

@Component
public class PriceStore {

    private final ConcurrentHashMap<String, AtomicReference<MarketTick>> ticks = new ConcurrentHashMap<>();

    public void updatePrice(String exchange, String symbol, double bid, double ask, double last, long ts) {
        ref(exchange, symbol).updateAndGet(prev ->
                new MarketTick(exchange, symbol, bid, ask, last, prev.bids(), prev.asks(), ts));
    }

    public void updateOrderBook(String exchange, String symbol,
                                List<MarketTick.OrderBookLevel> bids,
                                List<MarketTick.OrderBookLevel> asks,
                                long ts) {
        ref(exchange, symbol).updateAndGet(prev ->
                new MarketTick(exchange, symbol, prev.bid(), prev.ask(), prev.last(), bids, asks, ts));
    }

    public MarketTick get(String exchange, String symbol) {
        AtomicReference<MarketTick> ref = ticks.get(key(exchange, symbol));
        return ref == null ? null : ref.get();
    }

    public Collection<MarketTick> getAll() {
        return ticks.values().stream().map(AtomicReference::get).collect(Collectors.toList());
    }

    private AtomicReference<MarketTick> ref(String exchange, String symbol) {
        return ticks.computeIfAbsent(key(exchange, symbol),
                k -> new AtomicReference<>(MarketTick.empty(exchange, symbol)));
    }

    private String key(String exchange, String symbol) {
        return exchange + ":" + symbol;
    }
}