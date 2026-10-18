package com.MySelfAlgoWithLoan.prod.web;

import com.MySelfAlgoWithLoan.prod.web.model.CandidateSnapshot;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class CandidateStore {

    private final ConcurrentHashMap<String, CandidateSnapshot> candidates = new ConcurrentHashMap<>();

    public void update(CandidateSnapshot snapshot) {
        candidates.put(snapshot.symbol(), snapshot);
    }

    public void remove(String symbol) {
        candidates.remove(symbol);
    }

    public void clear() {
        candidates.clear();
    }

    public List<CandidateSnapshot> all() {
        return List.copyOf(candidates.values());
    }
}