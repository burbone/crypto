package com.MySelfAlgoWithLoan.prod.paper;

import com.MySelfAlgoWithLoan.prod.calc.model.EntrySide;
import com.MySelfAlgoWithLoan.prod.config.Properties;
import com.MySelfAlgoWithLoan.prod.util.SymbolFormat;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
@RequiredArgsConstructor
public class PaperTradeExecutor {

    private static final String USDT = "USDT";

    private final VirtualAccountBook accountBook;
    private final Properties properties;

    public void simulateEntry(String symbol, EntrySide side, double qty,
                              double futuresSellVwap, double spotBuyVwap) {
        String coin = SymbolFormat.coinFromSymbol(symbol);
        double feeFutures = feeForFutures(side);
        double feeSpot = feeForSpot(side);

        BigDecimal q = BigDecimal.valueOf(qty);

        VirtualExchangeAccount futuresAccount = futuresAccount(side);
        VirtualExchangeAccount spotAccount = spotAccount(side);

        futuresAccount.debit(USDT, BigDecimal.valueOf(qty * futuresSellVwap * feeFutures));
        spotAccount.debit(USDT, BigDecimal.valueOf(qty * spotBuyVwap * (1 + feeSpot)));
        spotAccount.credit(coin, q);
    }

    public void simulateExit(String symbol, EntrySide side, double qty,
                             double entryFuturesPrice,
                             double futuresBuyVwap, double spotSellVwap) {
        String coin = SymbolFormat.coinFromSymbol(symbol);
        double feeFutures = feeForFutures(side);
        double feeSpot = feeForSpot(side);

        VirtualExchangeAccount futuresAccount = futuresAccount(side);
        VirtualExchangeAccount spotAccount = spotAccount(side);

        double pnl = qty * (entryFuturesPrice - futuresBuyVwap);
        BigDecimal pnlAmount = BigDecimal.valueOf(pnl);
        if (pnl >= 0) {
            futuresAccount.credit(USDT, pnlAmount);
        } else {
            futuresAccount.debit(USDT, pnlAmount.abs());
        }
        futuresAccount.debit(USDT, BigDecimal.valueOf(qty * futuresBuyVwap * feeFutures));

        spotAccount.debit(coin, BigDecimal.valueOf(qty));
        spotAccount.credit(USDT, BigDecimal.valueOf(qty * spotSellVwap * (1 - feeSpot)));
    }

    private VirtualExchangeAccount futuresAccount(EntrySide side) {
        return switch (side) {
            case BYBIT_FUTURES_SHORT__KUCOIN_SPOT_LONG -> accountBook.bybit();
            case KUCOIN_FUTURES_SHORT__BYBIT_SPOT_LONG -> accountBook.kucoin();
        };
    }

    private VirtualExchangeAccount spotAccount(EntrySide side) {
        return switch (side) {
            case BYBIT_FUTURES_SHORT__KUCOIN_SPOT_LONG -> accountBook.kucoin();
            case KUCOIN_FUTURES_SHORT__BYBIT_SPOT_LONG -> accountBook.bybit();
        };
    }

    private double feeForFutures(EntrySide side) {
        return switch (side) {
            case BYBIT_FUTURES_SHORT__KUCOIN_SPOT_LONG -> properties.getBybitFuturesTakerFeePercent() / 100.0;
            case KUCOIN_FUTURES_SHORT__BYBIT_SPOT_LONG -> properties.getKucoinFuturesTakerFeePercent() / 100.0;
        };
    }

    private double feeForSpot(EntrySide side) {
        return switch (side) {
            case BYBIT_FUTURES_SHORT__KUCOIN_SPOT_LONG -> properties.getKucoinSpotTakerFeePercent() / 100.0;
            case KUCOIN_FUTURES_SHORT__BYBIT_SPOT_LONG -> properties.getBybitSpotTakerFeePercent() / 100.0;
        };
    }
}