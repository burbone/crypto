package com.MySelfAlgoWithLoan.prod.paper;

import com.MySelfAlgoWithLoan.prod.paper.PaperTradingProperties;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
@RequiredArgsConstructor
public class VirtualAccountBook {

    private static final String USDT = "USDT";

    private final PaperTradingProperties paperProperties;

    private final VirtualExchangeAccount bybit = new VirtualExchangeAccount();
    private final VirtualExchangeAccount kucoin = new VirtualExchangeAccount();

    @PostConstruct
    public void init() {
        bybit.credit(USDT, BigDecimal.valueOf(paperProperties.getInitialUsdtBalanceBybit()));
        kucoin.credit(USDT, BigDecimal.valueOf(paperProperties.getInitialUsdtBalanceKucoin()));
    }

    public VirtualExchangeAccount bybit() {
        return bybit;
    }

    public VirtualExchangeAccount kucoin() {
        return kucoin;
    }
}