package com.MySelfAlgoWithLoan.prod.service;

import com.MySelfAlgoWithLoan.prod.api.ws.*;
import com.MySelfAlgoWithLoan.prod.config.Properties;
import com.MySelfAlgoWithLoan.prod.util.SymbolFormat;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
@RequiredArgsConstructor
public class MarketDataStartupRunner implements CommandLineRunner {

    private final Properties properties;
    private final BybitSpotPriceClient bybitSpotPriceClient;
    private final BybitSpotDepthClient bybitSpotDepthClient;
    private final BybitFuturesPriceClient bybitFuturesPriceClient;
    private final BybitFuturesDepthClient bybitFuturesDepthClient;
    private final KuCoinSpotPriceClient kuCoinSpotPriceClient;
    private final KuCoinSpotDepthClient kuCoinSpotDepthClient;
    private final KuCoinFuturesPriceClient kuCoinFuturesPriceClient;
    private final KuCoinFuturesDepthClient kuCoinFuturesDepthClient;

    @Override
    public void run(String... args) {
        List<String> realPairs = new ArrayList<>(properties.getTrackedPairs());

        if (properties.isTestModeEnabled() && properties.getTestPairs() != null) {
            realPairs.removeAll(properties.getTestPairs());
        }

        bybitSpotPriceClient.connect(realPairs);
        bybitSpotDepthClient.connect(realPairs);
        bybitFuturesPriceClient.connect(realPairs);
        bybitFuturesDepthClient.connect(realPairs);

        List<String> kuCoinSpotSymbols = new ArrayList<>();
        for (String pair : realPairs) {
            kuCoinSpotSymbols.add(SymbolFormat.toKucoinSymbol(pair));
        }
        kuCoinSpotPriceClient.connect(kuCoinSpotSymbols);
        kuCoinSpotDepthClient.connect(kuCoinSpotSymbols);

        List<String> kuCoinFuturesSymbols = new ArrayList<>();
        for (String pair : realPairs) {
            String base = pair.replace("USDT", "");
            if ("BTC".equals(base)) {
                base = "XBT";
            }
            kuCoinFuturesSymbols.add(base + "USDTM");
        }
        kuCoinFuturesPriceClient.connect(kuCoinFuturesSymbols);
        kuCoinFuturesDepthClient.connect(kuCoinFuturesSymbols);
    }
}