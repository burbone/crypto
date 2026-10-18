package com.MySelfAlgoWithLoan.prod.calc;

import org.springframework.stereotype.Component;

@Component
public class CapitalHeadroomCalculator {

    public boolean hasHeadroom(double bybitUsdtBalance, double loanRatio, double feeBybit,
                               double qty, double bybitSellVwap,
                               double qRepayMax, double bybitBuyVwapAtRepayMax) {
        double headroom = (bybitUsdtBalance - loanRatio * bybitUsdtBalance)
                + qty * bybitSellVwap * (1 - feeBybit);

        double required = qRepayMax * bybitBuyVwapAtRepayMax * (1 + feeBybit);

        return headroom >= required;
    }
}