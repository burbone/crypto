package com.MySelfAlgoWithLoan.prod.strategy;

import com.MySelfAlgoWithLoan.prod.domain.MarketTick;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class NoOpTradingStrategy {

    @Bean
    @ConditionalOnMissingBean(TradingStrategy.class)
    public TradingStrategy noOpStrategy() {
        return new TradingStrategy() {
            @Override
            public void onTick(String symbol,
                               MarketTick bybitSpot,
                               MarketTick bybitFut,
                               MarketTick kucoinSpot,
                               MarketTick kucoinFut) {
            }
        };
    }
}