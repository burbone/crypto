package com.MySelfAlgoWithLoan.prod.strategy;

import com.MySelfAlgoWithLoan.prod.domain.MarketTick;

public interface TradingStrategy {

    void onTick(String symbol,
                MarketTick bybitSpot,
                MarketTick bybitFut,
                MarketTick kucoinSpot,
                MarketTick kucoinFut);
}