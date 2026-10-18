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
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.WebSocket;
import java.util.*;
import java.util.concurrent.CompletionStage;

@Component
@RequiredArgsConstructor
public class KuCoinFuturesDepthClient extends WsClientBase {

    private static final String BULLET_PUBLIC_URL = "https://api-futures.kucoin.com/api/v1/bullet-public";
    private static final int ORDERBOOK_DEPTH = 50;

    private final PriceStore priceStore;
    private final HttpClient httpClient = HttpClient.newHttpClient();
    private String currentToken;

    @Override
    protected void doConnect() {
        currentToken = fetchToken();
        for (List<String> chunk : chunks) {
            connectChunk(chunk);
        }
    }

    @Override
    protected void connectChunk(List<String> chunk) {
        String wsUrl = "wss://ws-api-futures.kucoin.com/?token=" + currentToken;
        String joinedSymbols = String.join(",", chunk);
        String subscribeMsg = "{\"id\":\"" + System.currentTimeMillis()
                + "\",\"type\":\"subscribe\",\"topic\":\"/contractMarket/level2Depth" + ORDERBOOK_DEPTH + ":" + joinedSymbols
                + "\",\"privateChannel\":false,\"response\":true}";

        httpClient.newWebSocketBuilder()
                .buildAsync(URI.create(wsUrl), new WebSocket.Listener() {
                    private final StringBuilder buffer = new StringBuilder();

                    @Override
                    public void onOpen(WebSocket webSocket) {
                        registerWebSocket(webSocket, chunk);
                        webSocket.sendText(subscribeMsg, true);
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
                        String msg = error.getMessage();
                        if (msg != null && msg.contains("401")) {
                            System.out.println("[" + getClass().getSimpleName() + "] 401 detected, restarting...");
                            restart();
                            return;
                        }
                        List<String> chunk = symbolsBySocket.get(webSocket);
                        if (chunk != null) {
                            removeWebSocket(webSocket);
                            reconnectChunk(chunk);
                        }
                    }
                });
    }

    private String fetchToken() {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(BULLET_PUBLIC_URL))
                    .POST(HttpRequest.BodyPublishers.noBody())
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            JsonObject data = JsonParser.parseString(response.body()).getAsJsonObject().getAsJsonObject("data");
            return data.get("token").getAsString();
        } catch (Exception e) {
            throw new RuntimeException("Failed to get KuCoin Futures token", e);
        }
    }

    @Override
    protected void startPing() {
        schedulePing(this::sendPingToAll);
    }

    @Override
    protected void sendPing(WebSocket ws) {
        ws.sendText("{\"type\":\"ping\"}", true);
    }

    private void handleMessage(String raw) {
        JsonObject json = JsonParser.parseString(raw).getAsJsonObject();
        if (!json.has("type") || !"message".equals(json.get("type").getAsString())) return;

        String topic = json.get("topic").getAsString();
        if (!topic.startsWith("/contractMarket/level2Depth")) return;

        String symbol = topic.substring(topic.indexOf(':') + 1);
        JsonObject data = json.getAsJsonObject("data");
        if (data == null) return;

        JsonArray bidsArray = data.getAsJsonArray("bids");
        JsonArray asksArray = data.getAsJsonArray("asks");

        List<MarketTick.OrderBookLevel> bids = parseLevels(bidsArray);
        List<MarketTick.OrderBookLevel> asks = parseLevels(asksArray);

        priceStore.updateOrderBook("KUCOIN_FUTURES", symbol, bids, asks, System.currentTimeMillis());

        if (!bids.isEmpty() && !asks.isEmpty()) {
            MarketTick current = priceStore.get("KUCOIN_FUTURES", symbol);
            double last = current != null ? current.last() : 0.0;
            priceStore.updatePrice("KUCOIN_FUTURES", symbol, bids.get(0).price(), asks.get(0).price(), last, System.currentTimeMillis());
        }
    }

    private List<MarketTick.OrderBookLevel> parseLevels(JsonArray levels) {
        List<MarketTick.OrderBookLevel> result = new ArrayList<>();
        for (int i = 0; i < levels.size(); i++) {
            JsonArray level = levels.get(i).getAsJsonArray();
            double price = level.get(0).getAsDouble();
            double size = level.get(1).getAsDouble();
            result.add(new MarketTick.OrderBookLevel(price, size));
        }
        return result;
    }
}