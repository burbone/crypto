package com.MySelfAlgoWithLoan.prod.api.ws;

import com.MySelfAlgoWithLoan.prod.domain.MarketTick;
import com.MySelfAlgoWithLoan.prod.marketdata.PriceStore;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.WebSocket;
import java.util.List;
import java.util.concurrent.CompletionStage;

@Component
@RequiredArgsConstructor
public class BybitSpotPriceClient extends WsClientBase {

    private static final String WS_URL = "wss://stream.bybit.com/v5/public/spot";

    private final PriceStore priceStore;
    private final HttpClient httpClient = HttpClient.newHttpClient();

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
            args.append("\"tickers.").append(s).append("\"");
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
        if (!json.has("topic") || !json.get("topic").getAsString().startsWith("tickers.")) return;

        JsonObject data = json.getAsJsonObject("data");
        if (data == null) return;

        String symbol = data.has("symbol") ? data.get("symbol").getAsString() : "";
        if (symbol.isEmpty()) return;

        double bid = data.has("bid1Price") ? data.get("bid1Price").getAsDouble() : 0.0;
        double ask = data.has("ask1Price") ? data.get("ask1Price").getAsDouble() : 0.0;
        double last = data.has("lastPrice") ? data.get("lastPrice").getAsDouble() : 0.0;

        priceStore.updatePrice("BYBIT", symbol, bid, ask, last, System.currentTimeMillis());
    }
}