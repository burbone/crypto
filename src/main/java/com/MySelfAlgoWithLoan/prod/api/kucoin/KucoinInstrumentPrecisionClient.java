package com.MySelfAlgoWithLoan.prod.api.kucoin;

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
public class KucoinInstrumentPrecisionClient {

    private static final String SUCCESS_CODE = "200000";

    private final Properties properties;

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    private final ConcurrentHashMap<String, BigDecimal> qtyStepCache = new ConcurrentHashMap<>();

    public BigDecimal getBaseQtyStep(String symbol) {
        return qtyStepCache.computeIfAbsent(symbol, this::fetchBaseQtyStep);
    }

    private BigDecimal fetchBaseQtyStep(String symbol) {
        String path = "/api/v2/symbols/" + symbol;
        String url = properties.getKucoin().getBaseUrl() + path;

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
            throw new ExchangeApiException("KUCOIN", path, null, null,
                    "network error while fetching instrument precision: " + e.getMessage(), null, e);
        }

        String rawBody = response.body();
        if (response.statusCode() != 200) {
            throw new ExchangeApiException("KUCOIN", path, response.statusCode(), null,
                    "unexpected HTTP status while fetching instrument precision", rawBody, null);
        }

        JsonObject result;
        try {
            result = JsonParser.parseString(rawBody).getAsJsonObject();
        } catch (Exception e) {
            throw new ExchangeApiException("KUCOIN", path, response.statusCode(), null,
                    "symbol-info response is not valid JSON", rawBody, e);
        }

        String code = result.has("code") ? result.get("code").getAsString() : null;
        if (!SUCCESS_CODE.equals(code)) {
            String msg = result.has("msg") ? result.get("msg").getAsString() : null;
            throw new ExchangeApiException("KUCOIN", path, response.statusCode(), code, msg, rawBody, null);
        }

        String baseIncrement = result.getAsJsonObject("data").get("baseIncrement").getAsString();
        return new BigDecimal(baseIncrement);
    }
}