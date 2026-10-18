package com.MySelfAlgoWithLoan.prod.paper;

import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class PositionRegistry {

    private final ConcurrentHashMap<String, OpenPosition> openPositions = new ConcurrentHashMap<>();

    public boolean hasOpenPosition(String symbol) {
        return openPositions.containsKey(symbol);
    }

    public OpenPosition getOpenPosition(String symbol) {
        return openPositions.get(symbol);
    }

    public void open(OpenPosition position) {
        openPositions.put(position.symbol(), position);
    }

    public void close(String symbol) {
        openPositions.remove(symbol);
    }

    public List<OpenPosition> allOpenPositions() {
        return List.copyOf(openPositions.values());
    }
}