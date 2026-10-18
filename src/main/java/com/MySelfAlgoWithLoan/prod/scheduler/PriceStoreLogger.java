package com.MySelfAlgoWithLoan.prod.scheduler;

import com.MySelfAlgoWithLoan.prod.domain.MarketTick;
import com.MySelfAlgoWithLoan.prod.marketdata.PriceStore;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class PriceStoreLogger {

    private final PriceStore priceStore;

    @Scheduled(fixedRate = 1800000)
    public void printCollectedData() {
        System.out.println("Price taker - good");
    }
}