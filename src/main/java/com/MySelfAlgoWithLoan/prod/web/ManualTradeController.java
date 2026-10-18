package com.MySelfAlgoWithLoan.prod.web;

import com.MySelfAlgoWithLoan.prod.calc.model.EntrySide;
import com.MySelfAlgoWithLoan.prod.orchestrator.TradeOrchestrator;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class ManualTradeController {

    private final TradeOrchestrator orchestrator;

    @PostMapping("/manual-entry")
    public String manualEntry(@RequestParam String symbol, @RequestParam String side) {
        try {
            EntrySide entrySide = EntrySide.valueOf(side);
            return orchestrator.manualEntryDetailed(symbol, entrySide);
        } catch (IllegalArgumentException e) {
            return "Неверная сторона: " + side;
        }
    }
}