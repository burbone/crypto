package com.MySelfAlgoWithLoan.prod.calc;

import com.MySelfAlgoWithLoan.prod.calc.model.ExitDecision;
import org.springframework.stereotype.Component;

@Component
public class ExitEvaluator {

    public ExitDecision evaluate(double currentProfitPercent,
                                 double currentProfitUsdt,
                                 boolean profitTargetReached,
                                 double maxProfitPercent,
                                 double metric) {
        return ExitDecision.hold();
    }
}