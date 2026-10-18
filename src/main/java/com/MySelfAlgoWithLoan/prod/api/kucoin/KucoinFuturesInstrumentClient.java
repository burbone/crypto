package com.MySelfAlgoWithLoan.prod.api.kucoin;

import com.MySelfAlgoWithLoan.prod.api.ExchangeApiException;
import com.MySelfAlgoWithLoan.prod.config.Properties;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.math.BigDecimal;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

@Component
@RequiredArgsConstructor
public class KucoinFuturesInstrumentClient {

    private final Properties properties;

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    private final ConcurrentHashMap<String, ContractInfo> cache = new ConcurrentHashMap<>();
    private volatile List<ContractInfo> allContracts;

    public ContractInfo getContractInfo(String spotSymbol) {
        return cache.computeIfAbsent(spotSymbol, this::resolveContract);
    }

    private ContractInfo resolveContract(String spotSymbol) {
        if (allContracts == null) {
            allContracts = fetchActiveContracts();
        }

        String baseCoin = spotSymbol.split("-")[0];
        String futuresBase = "BTC".equals(baseCoin) ? "XBT" : baseCoin;
        for (ContractInfo contract : allContracts) {
            if (futuresBase.equals(contract.baseCurrency()) && "USDT".equals(contract.quoteCurrency())) {
                return contract;
            }
        }
        throw new ExchangeApiException("KUCOIN_FUTURES", "/api/v1/contracts/active", null, null,
                "futures contract not found for spot symbol " + spotSymbol, null, null);
    }

    private List<ContractInfo> fetchActiveContracts() {
        String url = properties.getKucoin().getFuturesBaseUrl() + "/api/v1/contracts/active";
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(Duration.ofSeconds(15))
                .GET()
                .build();

        HttpResponse<String> response;
        try {
            response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        } catch (IOException | InterruptedException e) {
            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            throw new ExchangeApiException("KUCOIN_FUTURES", "/api/v1/contracts/active", null, null,
                    "network error while fetching contracts: " + e.getMessage(), null, e);
        }

        String rawBody = response.body();
        if (response.statusCode() != 200) {
            throw new ExchangeApiException("KUCOIN_FUTURES", "/api/v1/contracts/active", response.statusCode(), null,
                    "unexpected HTTP status while fetching contracts", rawBody, null);
        }

        JsonObject result;
        try {
            result = JsonParser.parseString(rawBody).getAsJsonObject();
        } catch (Exception e) {
            throw new ExchangeApiException("KUCOIN_FUTURES", "/api/v1/contracts/active", response.statusCode(), null,
                    "contracts response is not valid JSON", rawBody, e);
        }

        String code = result.has("code") ? result.get("code").getAsString() : null;
        if (!"200000".equals(code)) {
            String msg = result.has("msg") ? result.get("msg").getAsString() : null;
            throw new ExchangeApiException("KUCOIN_FUTURES", "/api/v1/contracts/active", response.statusCode(), code, msg, rawBody, null);
        }

        JsonArray data = result.getAsJsonArray("data");
        List<ContractInfo> list = new ArrayList<>();
        for (int i = 0; i < data.size(); i++) {
            JsonObject c = data.get(i).getAsJsonObject();
            String symbol = c.get("symbol").getAsString();
            String baseCurrency = c.get("baseCurrency").getAsString();
            String quoteCurrency = c.get("quoteCurrency").getAsString();
            BigDecimal multiplier = new BigDecimal(c.get("multiplier").getAsString());
            BigDecimal lotSize = new BigDecimal(c.get("lotSize").getAsString());
            int maxOrderQty = c.get("maxOrderQty").getAsInt();
            list.add(new ContractInfo(symbol, baseCurrency, quoteCurrency, multiplier, lotSize, maxOrderQty));
        }
        return list;
    }

    public record ContractInfo(String symbol, String baseCurrency, String quoteCurrency,
                               BigDecimal multiplier, BigDecimal lotSize, int maxOrderQty) {}
}