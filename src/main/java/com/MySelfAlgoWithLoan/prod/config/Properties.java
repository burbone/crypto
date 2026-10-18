package com.MySelfAlgoWithLoan.prod.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

@Data
@ConfigurationProperties(prefix = "arbitrage")
public class Properties {

    private double entryGatePrimary;
    private double entryGateSecondary;
    private double exitGatePrimary;
    private double exitGateSecondary;
    private double exitGateTertiary;

    private double loanRatioOfBybitBalance;
    private int positionTimeoutSeconds;
    private int banDurationHours;
    private int loanRateCacheSeconds;

    private double bybitSpotTakerFeePercent;
    private double bybitFuturesTakerFeePercent;
    private double kucoinSpotTakerFeePercent;
    private double kucoinFuturesTakerFeePercent;

    private double maxSlippagePercent;
    private double minLoanAmountUsdt;

    private double riskLimitPrimary;
    private double riskLimitSecondary;

    private List<String> trackedPairs;
    private List<String> testPairs;
    private boolean testModeEnabled;

    private int marketDataRestartMinutes;

    public String getMarketDataRestartMillis() {
        return String.valueOf((long) marketDataRestartMinutes * 60_000L);
    }

    private Bybit bybit;
    private Kucoin kucoin;

    @Data
    public static class Bybit {
        private String apiKey;
        private String apiSecret;
        private String baseUrl;
        private String wsUrl;
    }

    @Data
    public static class Kucoin {
        private String apiKey;
        private String apiSecret;
        private String apiPassphrase;
        private String baseUrl;
        private String futuresBaseUrl;
    }
}