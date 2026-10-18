package com.MySelfAlgoWithLoan.prod.api.bybit;

import com.MySelfAlgoWithLoan.prod.api.ExchangeApiException;
import com.MySelfAlgoWithLoan.prod.config.Properties;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;


@Component
@RequiredArgsConstructor
public class BybitRestClient {

    private static final String RECV_WINDOW = "5000";
    private static final int SUCCESS_RET_CODE = 0;

    private final Properties properties;
    private final BybitSigner signer;

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    public JsonObject post(String path, JsonObject body) {
        String bodyJson = body.toString();
        String timestamp = String.valueOf(System.currentTimeMillis());
        String apiKey = properties.getBybit().getApiKey();
        String secret = properties.getBybit().getApiSecret();

        String signPayload = timestamp + apiKey + RECV_WINDOW + bodyJson;
        String signature = signer.sign(secret, signPayload);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(properties.getBybit().getBaseUrl() + path))
                .timeout(Duration.ofSeconds(15))
                .header("Content-Type", "application/json")
                .header("X-BAPI-API-KEY", apiKey)
                .header("X-BAPI-TIMESTAMP", timestamp)
                .header("X-BAPI-RECV-WINDOW", RECV_WINDOW)
                .header("X-BAPI-SIGN", signature)
                .POST(HttpRequest.BodyPublishers.ofString(bodyJson))
                .build();

        HttpResponse<String> response;
        try {
            response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        } catch (IOException | InterruptedException e) {
            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            throw new ExchangeApiException("BYBIT", path, null, null,
                    "network error: " + e.getMessage(), null, e);
        }

        String rawBody = response.body();

        if (response.statusCode() != 200) {
            throw new ExchangeApiException("BYBIT", path, response.statusCode(), null,
                    "unexpected HTTP status", rawBody, null);
        }

        JsonObject result;
        try {
            result = JsonParser.parseString(rawBody).getAsJsonObject();
        } catch (Exception e) {
            throw new ExchangeApiException("BYBIT", path, response.statusCode(), null,
                    "response is not valid JSON", rawBody, e);
        }

        System.out.println("[Bybit] " + path + " -> " + result);

        if (!result.has("retCode")) {
            throw new ExchangeApiException("BYBIT", path, response.statusCode(), null,
                    "response has no retCode field", rawBody, null);
        }

        int retCode = result.get("retCode").getAsInt();
        if (retCode != SUCCESS_RET_CODE) {
            String retMsg = result.has("retMsg") ? result.get("retMsg").getAsString() : null;
            throw new ExchangeApiException("BYBIT", path, response.statusCode(),
                    String.valueOf(retCode), retMsg, rawBody, null);
        }

        return result;
    }
}