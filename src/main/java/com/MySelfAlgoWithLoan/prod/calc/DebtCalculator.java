package com.MySelfAlgoWithLoan.prod.calc;

import org.springframework.stereotype.Component;

@Component
public class DebtCalculator {

    public double repayQty(double qty, double ratePerMinute, double minutesHeld) {
        return qty * (1 + ratePerMinute * minutesHeld);
    }
}