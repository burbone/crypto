package com.MySelfAlgoWithLoan.prod.calc;

import com.MySelfAlgoWithLoan.prod.domain.MarketTick;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class VwapCalculator {

    public double vwap(List<MarketTick.OrderBookLevel> levels, double qty) {
        double remaining = qty;
        double notional = 0;
        double filledQty = 0;

        for (MarketTick.OrderBookLevel level : levels) {
            if (remaining <= 0) {
                break;
            }
            double fill = Math.min(remaining, level.size());
            notional += fill * level.price();
            filledQty += fill;
            remaining -= fill;
        }

        if (filledQty == 0) {
            return 0.0;
        }
        return notional / filledQty;
    }

    public double getAvailableQty(List<MarketTick.OrderBookLevel> levels) {
        double available = 0;
        for (MarketTick.OrderBookLevel level : levels) {
            available += level.size();
        }
        return available;
    }

    public double maxDepthQty(List<MarketTick.OrderBookLevel> levels, double referencePrice, double maxSlippagePercent) {
        double cumulativeQty = 0;
        double cumulativeNotional = 0;

        for (MarketTick.OrderBookLevel level : levels) {
            double candidateQty = cumulativeQty + level.size();
            double candidateNotional = cumulativeNotional + level.size() * level.price();
            double candidateVwap = candidateNotional / candidateQty;

            double slippagePercent = Math.abs(candidateVwap / referencePrice - 1) * 100;
            if (slippagePercent > maxSlippagePercent) {
                break;
            }

            cumulativeQty = candidateQty;
            cumulativeNotional = candidateNotional;
        }

        return cumulativeQty;
    }
}