package com.MySelfAlgoWithLoan.prod.api.ws;

import com.MySelfAlgoWithLoan.prod.domain.MarketTick;
import com.MySelfAlgoWithLoan.prod.marketdata.PriceStore;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.WebSocket;
import java.util.*;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class BybitSpotDepthClient extends WsClientBase {

    private static final String WS_URL = "wss://stream.bybit.com/v5/public/spot";
    private static final int ORDERBOOK_DEPTH = 200;

    private final PriceStore priceStore;
    private final HttpClient httpClient = HttpClient.newHttpClient();
    private final Map<String, OrderBook> orderBooks = new ConcurrentHashMap<>();

    @Override
    protected void doConnect() {
        for (List<String> chunk : chunks) {
            connectChunk(chunk);
        }
    }

    @Override
    protected void connectChunk(List<String> chunk) {
        StringBuilder args = new StringBuilder();
        for (String s : chunk) {
            if (args.length() > 0) args.append(",");
            args.append("\"orderbook.").append(ORDERBOOK_DEPTH).append(".").append(s).append("\"");
        }
        String subscribeMessage = "{\"op\":\"subscribe\",\"args\":[" + args + "]}";

        httpClient.newWebSocketBuilder()
                .buildAsync(URI.create(WS_URL), new WebSocket.Listener() {
                    private final StringBuilder buffer = new StringBuilder();

                    @Override
                    public void onOpen(WebSocket webSocket) {
                        registerWebSocket(webSocket, chunk);
                        webSocket.sendText(subscribeMessage, true);
                        WebSocket.Listener.super.onOpen(webSocket);
                    }

                    @Override
                    public CompletionStage<?> onText(WebSocket webSocket, CharSequence data, boolean last) {
                        updateLastMessageTime(webSocket);
                        buffer.append(data);
                        if (last) {
                            handleMessage(buffer.toString());
                            buffer.setLength(0);
                        }
                        webSocket.request(1);
                        return null;
                    }

                    @Override
                    public CompletionStage<?> onClose(WebSocket webSocket, int statusCode, String reason) {
                        markCloseReason(webSocket, statusCode, reason);
                        List<String> chunk = symbolsBySocket.get(webSocket);
                        if (chunk != null) {
                            removeWebSocket(webSocket);
                            reconnectChunk(chunk);
                        }
                        return WebSocket.Listener.super.onClose(webSocket, statusCode, reason);
                    }

                    @Override
                    public void onError(WebSocket webSocket, Throwable error) {
                        markErrorReason(webSocket, error.getMessage());
                        List<String> chunk = symbolsBySocket.get(webSocket);
                        if (chunk != null) {
                            removeWebSocket(webSocket);
                            reconnectChunk(chunk);
                        }
                    }
                });
    }

    @Override
    protected void startPing() {
        schedulePing(this::sendPingToAll);
    }

    @Override
    protected void sendPing(WebSocket ws) {
        ws.sendText("ping", true);
    }

    private void handleMessage(String raw) {
        JsonObject json = JsonParser.parseString(raw).getAsJsonObject();
        if (!json.has("topic") || !json.get("topic").getAsString().startsWith("orderbook.")) return;

        JsonObject data = json.getAsJsonObject("data");
        if (data == null) return;

        String symbol = data.get("s").getAsString();
        String type = json.get("type").getAsString();
        OrderBook book = orderBooks.computeIfAbsent(symbol, s -> new OrderBook());

        if ("snapshot".equals(type)) {
            book.applySnapshot(data.getAsJsonArray("b"), data.getAsJsonArray("a"));
        } else {
            book.applyDelta(data.getAsJsonArray("b"), data.getAsJsonArray("a"));
        }

        List<MarketTick.OrderBookLevel> topBids = book.topBids();
        List<MarketTick.OrderBookLevel> topAsks = book.topAsks();
        priceStore.updateOrderBook("BYBIT", symbol, topBids, topAsks, System.currentTimeMillis());

        if (!topBids.isEmpty() && !topAsks.isEmpty()) {
            MarketTick current = priceStore.get("BYBIT", symbol);
            double last = current != null ? current.last() : 0.0;
            priceStore.updatePrice("BYBIT", symbol, topBids.get(0).price(), topAsks.get(0).price(), last, System.currentTimeMillis());
        }
    }

    private static class OrderBook {
        private final NavigableMap<Double, Double> bids = new ConcurrentSkipListMap<>(Comparator.reverseOrder());
        private final NavigableMap<Double, Double> asks = new ConcurrentSkipListMap<>();

        synchronized void applySnapshot(JsonArray bidLevels, JsonArray askLevels) {
            bids.clear();
            asks.clear();
            apply(bidLevels, bids);
            apply(askLevels, asks);
        }

        synchronized void applyDelta(JsonArray bidLevels, JsonArray askLevels) {
            apply(bidLevels, bids);
            apply(askLevels, asks);
        }

        private void apply(JsonArray levels, NavigableMap<Double, Double> book) {
            for (int i = 0; i < levels.size(); i++) {
                JsonArray level = levels.get(i).getAsJsonArray();
                double price = level.get(0).getAsDouble();
                double size = level.get(1).getAsDouble();
                if (size == 0) book.remove(price);
                else book.put(price, size);
            }
        }

        synchronized List<MarketTick.OrderBookLevel> topBids() {
            return bids.entrySet().stream()
                    .map(e -> new MarketTick.OrderBookLevel(e.getKey(), e.getValue()))
                    .collect(Collectors.toList());
        }

        synchronized List<MarketTick.OrderBookLevel> topAsks() {
            return asks.entrySet().stream()
                    .map(e -> new MarketTick.OrderBookLevel(e.getKey(), e.getValue()))
                    .collect(Collectors.toList());
        }
    }
}