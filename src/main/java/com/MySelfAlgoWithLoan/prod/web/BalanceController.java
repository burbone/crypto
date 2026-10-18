package com.MySelfAlgoWithLoan.prod.web;

import com.MySelfAlgoWithLoan.prod.paper.PortfolioValuationService;
import com.MySelfAlgoWithLoan.prod.web.model.BalanceSnapshot;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class BalanceController {

    private final PortfolioValuationService valuationService;

    @GetMapping("/api/balances")
    public BalanceSnapshot balances() {
        return valuationService.snapshot();
    }
}