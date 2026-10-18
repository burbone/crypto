package com.MySelfAlgoWithLoan.prod;

import com.MySelfAlgoWithLoan.prod.paper.PaperTradingProperties;
import com.MySelfAlgoWithLoan.prod.config.Properties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableConfigurationProperties({Properties.class, PaperTradingProperties.class})
@EnableScheduling
public class ProdApplication {
	public static void main(String[] args) {
		SpringApplication.run(ProdApplication.class, args);
	}
}
