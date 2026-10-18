package com.MySelfAlgoWithLoan.prod.api.bybit;

import com.MySelfAlgoWithLoan.prod.api.ExchangeApiException;
import com.MySelfAlgoWithLoan.prod.config.Properties;
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
import java.util.concurrent.ConcurrentHashMap;

@Component
@RequiredArgsConstructor
public class BybitInstrumentPrecisionClient {

    private final Properties properties;

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    private final ConcurrentHashMap<String, BigDecimal> qtyStepCache = new ConcurrentHashMap<>();

    public BigDecimal getBaseQtyStep(String symbol) {
        return getBaseQtyStep(symbol, "spot");
    }

    public BigDecimal getBaseQtyStep(String symbol, String category) {
        String cacheKey = category + ":" + symbol;
        return qtyStepCache.computeIfAbsent(cacheKey, k -> fetchBaseQtyStep(symbol, category));
    }

    private BigDecimal fetchBaseQtyStep(String symbol, String category) {
        String path = "/v5/market/instruments-info";
        String url = properties.getBybit().getBaseUrl() + path + "?category=" + category + "&symbol=" + symbol;

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
            throw new ExchangeApiException("BYBIT", path, null, null,
                    "network error while fetching instrument precision: " + e.getMessage(), null, e);
        }

        String rawBody = response.body();
        if (response.statusCode() != 200) {
            throw new ExchangeApiException("BYBIT", path, response.statusCode(), null,
                    "unexpected HTTP status while fetching instrument precision", rawBody, null);
        }

        JsonObject result;
        try {
            result = JsonParser.parseString(rawBody).getAsJsonObject();
        } catch (Exception e) {
            throw new ExchangeApiException("BYBIT", path, response.statusCode(), null,
                    "instrument-info response is not valid JSON", rawBody, e);
        }

        int retCode = result.has("retCode") ? result.get("retCode").getAsInt() : -1;
        if (retCode != 0) {
            String retMsg = result.has("retMsg") ? result.get("retMsg").getAsString() : null;
            throw new ExchangeApiException("BYBIT", path, response.statusCode(),
                    String.valueOf(retCode), retMsg, rawBody, null);
        }

        JsonObject lotSizeFilter = result.getAsJsonObject("result").getAsJsonArray("list").get(0).getAsJsonObject()
                .getAsJsonObject("lotSizeFilter");
        String step;
        if ("linear".equals(category)) {
            step = lotSizeFilter.get("qtyStep").getAsString();
        } else {
            step = lotSizeFilter.get("basePrecision").getAsString();
        }
        return new BigDecimal(step);
    }
}