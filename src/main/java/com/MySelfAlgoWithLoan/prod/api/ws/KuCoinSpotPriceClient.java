package com.MySelfAlgoWithLoan.prod.api.ws;

import com.MySelfAlgoWithLoan.prod.domain.MarketTick;
import com.MySelfAlgoWithLoan.prod.marketdata.PriceStore;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.WebSocket;
import java.util.List;
import java.util.concurrent.CompletionStage;

@Component
@RequiredArgsConstructor
public class KuCoinSpotPriceClient extends WsClientBase {

    private static final String BULLET_PUBLIC_URL = "https://api.kucoin.com/api/v1/bullet-public";

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
        String wsUrl = "wss://ws-api-spot.kucoin.com/?token=" + currentToken;
        String joinedSymbols = String.join(",", chunk);
        String subscribeMsg = "{\"id\":\"" + System.currentTimeMillis()
                + "\",\"type\":\"subscribe\",\"topic\":\"/market/ticker:" + joinedSymbols
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
            throw new RuntimeException("Failed to get KuCoin token", e);
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
        if (!topic.startsWith("/market/ticker:")) return;

        String symbol = topic.substring(topic.indexOf(':') + 1);
        JsonObject data = json.getAsJsonObject("data");
        if (data == null) return;

        double bid = data.has("bestBid") ? data.get("bestBid").getAsDouble() : 0.0;
        double ask = data.has("bestAsk") ? data.get("bestAsk").getAsDouble() : 0.0;
        double last = data.has("price") ? data.get("price").getAsDouble() : 0.0;

        priceStore.updatePrice("KUCOIN", symbol, bid, ask, last, System.currentTimeMillis());
    }
}