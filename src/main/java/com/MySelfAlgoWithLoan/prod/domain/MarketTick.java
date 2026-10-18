package com.MySelfAlgoWithLoan.prod.domain;

import java.util.List;

public record MarketTick(
        String exchange,
        String symbol,
        double bid,
        double ask,
        double last,
        List<OrderBookLevel> bids,
        List<OrderBookLevel> asks,
        long timestamp
) {

    public static MarketTick empty(String exchange, String symbol) {
        return new MarketTick(exchange, symbol, 0, 0, 0, List.of(), List.of(), 0);
    }

    public record OrderBookLevel(double price, double size) {
    }
}