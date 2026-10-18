package com.MySelfAlgoWithLoan.prod.paper;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "paper")
public class PaperTradingProperties {

    private double initialUsdtBalanceBybit = 1000;
    private double initialUsdtBalanceKucoin = 1000;
}