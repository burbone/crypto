package com.MySelfAlgoWithLoan.prod.api.ws;

import java.net.http.WebSocket;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;

public abstract class WsClientBase {

    protected List<String> symbols;
    protected List<List<String>> chunks;
    protected final List<WebSocket> webSockets = new CopyOnWriteArrayList<>();
    protected final Map<WebSocket, List<String>> symbolsBySocket = new ConcurrentHashMap<>();
    protected final Map<WebSocket, Long> lastMessageTimeBySocket = new ConcurrentHashMap<>();
    protected final Map<WebSocket, String> lastCloseReasonBySocket = new ConcurrentHashMap<>();

    private final ConcurrentHashMap<List<String>, Boolean> chunkReconnectLock = new ConcurrentHashMap<>();

    protected ScheduledExecutorService pingScheduler;
    protected ScheduledExecutorService watchdogScheduler;

    protected long staleTimeoutMs = 30_000;

    public void connect(List<String> symbols) {
        this.symbols = new ArrayList<>(symbols);
        this.chunks = splitIntoChunks(this.symbols, 3);
        doConnect();
        startPing();
        startWatchdog();
    }

    public void restart() {
        shutdown();
        connect(getSymbols());
    }

    public List<String> getSymbols() {
        return symbols;
    }

    protected void registerWebSocket(WebSocket webSocket, List<String> chunkSymbols) {
        webSockets.add(webSocket);
        symbolsBySocket.put(webSocket, new ArrayList<>(chunkSymbols));
        lastMessageTimeBySocket.put(webSocket, System.currentTimeMillis());
        lastCloseReasonBySocket.put(webSocket, "");
    }

    protected void updateLastMessageTime(WebSocket webSocket) {
        lastMessageTimeBySocket.put(webSocket, System.currentTimeMillis());
    }

    protected void markCloseReason(WebSocket webSocket, int statusCode, String reason) {
        lastCloseReasonBySocket.put(webSocket, "close=" + statusCode + " " + reason);
    }

    protected void markErrorReason(WebSocket webSocket, String errorMessage) {
        lastCloseReasonBySocket.put(webSocket, "error=" + errorMessage);
    }

    protected void removeWebSocket(WebSocket webSocket) {
        webSockets.remove(webSocket);
        symbolsBySocket.remove(webSocket);
        lastMessageTimeBySocket.remove(webSocket);
        lastCloseReasonBySocket.remove(webSocket);
    }

    protected void reconnectChunk(List<String> chunkSymbols) {
        if (chunkReconnectLock.putIfAbsent(chunkSymbols, true) != null) {
            return;
        }
        try {
            for (WebSocket ws : new ArrayList<>(webSockets)) {
                List<String> syms = symbolsBySocket.get(ws);
                if (syms != null && syms.equals(chunkSymbols)) {
                    removeWebSocket(ws);
                    try {
                        ws.sendClose(WebSocket.NORMAL_CLOSURE, "reconnect");
                    } catch (Exception ignored) {}
                }
            }
            connectChunk(chunkSymbols);
        } finally {
            chunkReconnectLock.remove(chunkSymbols);
        }
    }

    protected abstract void doConnect();
    protected abstract void startPing();
    protected abstract void sendPing(WebSocket ws);

    public void shutdown() {
        for (WebSocket ws : webSockets) {
            try {
                ws.sendClose(WebSocket.NORMAL_CLOSURE, "shutdown");
            } catch (Exception ignored) {}
        }
        webSockets.clear();
        symbolsBySocket.clear();
        lastMessageTimeBySocket.clear();
        lastCloseReasonBySocket.clear();

        if (pingScheduler != null && !pingScheduler.isShutdown()) {
            pingScheduler.shutdownNow();
        }
        if (watchdogScheduler != null && !watchdogScheduler.isShutdown()) {
            watchdogScheduler.shutdownNow();
        }
    }

    protected List<List<String>> splitIntoChunks(List<String> list, int chunkSize) {
        List<List<String>> result = new ArrayList<>();
        for (int i = 0; i < list.size(); i += chunkSize) {
            result.add(new ArrayList<>(list.subList(i, Math.min(i + chunkSize, list.size()))));
        }
        return result;
    }

    protected void schedulePing(Runnable pingTask) {
        pingScheduler = Executors.newSingleThreadScheduledExecutor();
        pingScheduler.scheduleAtFixedRate(pingTask, 15, 15, TimeUnit.SECONDS);
    }

    protected void startWatchdog() {
        if (watchdogScheduler != null && !watchdogScheduler.isShutdown()) {
            watchdogScheduler.shutdownNow();
        }
        watchdogScheduler = Executors.newSingleThreadScheduledExecutor();
        watchdogScheduler.scheduleAtFixedRate(() -> {
            long now = System.currentTimeMillis();
            for (Map.Entry<WebSocket, Long> entry : lastMessageTimeBySocket.entrySet()) {
                if (now - entry.getValue() > staleTimeoutMs) {
                    WebSocket ws = entry.getKey();
                    List<String> syms = symbolsBySocket.get(ws);
                    if (syms != null) {
                        System.out.println("[" + getClass().getSimpleName() + "] Stale symbols: " + syms
                                + " | reason: " + lastCloseReasonBySocket.getOrDefault(ws, "no close info"));
                        reconnectChunk(syms);
                    }
                }
            }
        }, staleTimeoutMs, staleTimeoutMs, TimeUnit.MILLISECONDS);
    }

    protected abstract void connectChunk(List<String> chunk);

    protected void sendPingToAll() {
        for (WebSocket ws : webSockets) {
            sendPing(ws);
        }
    }
}