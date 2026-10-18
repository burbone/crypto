package com.MySelfAlgoWithLoan.prod.paper;

import com.MySelfAlgoWithLoan.prod.config.Properties;
import com.MySelfAlgoWithLoan.prod.domain.MarketTick;
import com.MySelfAlgoWithLoan.prod.marketdata.PriceStore;
import com.MySelfAlgoWithLoan.prod.calc.model.EntrySide;
import com.MySelfAlgoWithLoan.prod.web.model.BalanceSnapshot;
import com.MySelfAlgoWithLoan.prod.web.model.OpenPositionSnapshot;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class PortfolioValuationService {

    private static final String USDT = "USDT";
    private static final String BYBIT = "BYBIT";
    private static final String KUCOIN = "KUCOIN";

    private final VirtualAccountBook accountBook;
    private final PriceStore priceStore;
    private final OpenPositionValuationService openPositionValuationService;
    private final com.MySelfAlgoWithLoan.prod.paper.PaperTradingProperties paperProperties;

    public BalanceSnapshot snapshot() {
        double bybitNetWorth = netWorth(accountBook.bybit(), BYBIT);
        double kucoinNetWorth = netWorth(accountBook.kucoin(), KUCOIN);

        List<OpenPositionSnapshot> openPositions = openPositionValuationService.snapshot();
        for (OpenPositionSnapshot pos : openPositions) {
            double profit = pos.currentProfitUsdt();
            String side = pos.side();
            if (side.startsWith("BYBIT_FUTURES")) {
                bybitNetWorth += profit;
            } else if (side.startsWith("KUCOIN_FUTURES")) {
                kucoinNetWorth += profit;
            }
        }

        double total = bybitNetWorth + kucoinNetWorth;

        double initialTotal = paperProperties.getInitialUsdtBalanceBybit()
                + paperProperties.getInitialUsdtBalanceKucoin();
        double changePercent = initialTotal == 0 ? 0 : (total - initialTotal) / initialTotal * 100;

        return new BalanceSnapshot(bybitNetWorth, kucoinNetWorth, total, changePercent);
    }

    private double netWorth(VirtualExchangeAccount account, String exchange) {
        double assets = 0;
        for (Map.Entry<String, BigDecimal> entry : account.allBalances().entrySet()) {
            assets += valueInUsdt(exchange, entry.getKey(), entry.getValue().doubleValue());
        }

        double liabilities = 0;
        for (Map.Entry<String, BigDecimal> entry : account.allDebts().entrySet()) {
            liabilities += valueInUsdt(exchange, entry.getKey(), entry.getValue().doubleValue());
        }

        return assets - liabilities;
    }

    private double valueInUsdt(String exchange, String currency, double amount) {
        if (USDT.equals(currency) || amount == 0) {
            return amount;
        }
        String symbol = BYBIT.equals(exchange) ? currency + USDT : currency + "-" + USDT;
        MarketTick tick = priceStore.get(exchange, symbol);
        if (tick == null || tick.bid() <= 0 || tick.ask() <= 0) {
            return 0;
        }
        double mid = (tick.bid() + tick.ask()) / 2;
        return amount * mid;
    }
}