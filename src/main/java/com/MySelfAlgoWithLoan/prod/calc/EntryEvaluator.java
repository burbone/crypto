package com.MySelfAlgoWithLoan.prod.calc;

import com.MySelfAlgoWithLoan.prod.calc.model.EntryDecision;
import com.MySelfAlgoWithLoan.prod.calc.model.EntrySide;
import com.MySelfAlgoWithLoan.prod.calc.model.PositionSizeResult;
import com.MySelfAlgoWithLoan.prod.calc.model.SpreadResult;
import org.springframework.stereotype.Component;

@Component
public class EntryEvaluator {

    public EntryDecision evaluate(String symbol,
                                  EntrySide side,
                                  PositionSizeResult sizeResult,
                                  SpreadResult spreadResult,
                                  boolean hasOpenPosition,
                                  boolean isBanned,
                                  double maxSpread) {
        return EntryDecision.reject(symbol, side, "Entry strategy not implemented");
    }
}