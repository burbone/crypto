package com.MySelfAlgoWithLoan.prod.api.kucoin;

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
public class KucoinFuturesRestClient {

    private static final String SUCCESS_CODE = "200000";

    private final Properties properties;
    private final KucoinSigner signer;

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    public JsonObject post(String path, JsonObject body) {
        String bodyJson = body.toString();
        String timestamp = String.valueOf(System.currentTimeMillis());
        String apiKey = properties.getKucoin().getApiKey();
        String secret = properties.getKucoin().getApiSecret();
        String passphrase = properties.getKucoin().getApiPassphrase();

        String signPayload = timestamp + "POST" + path + bodyJson;
        String signature = signer.sign(secret, signPayload);
        String signedPassphrase = signer.signPassphrase(secret, passphrase);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(properties.getKucoin().getFuturesBaseUrl() + path))
                .timeout(Duration.ofSeconds(15))
                .header("Content-Type", "application/json")
                .header("KC-API-KEY", apiKey)
                .header("KC-API-SIGN", signature)
                .header("KC-API-TIMESTAMP", timestamp)
                .header("KC-API-PASSPHRASE", signedPassphrase)
                .header("KC-API-KEY-VERSION", "2")
                .POST(HttpRequest.BodyPublishers.ofString(bodyJson))
                .build();

        HttpResponse<String> response;
        try {
            response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        } catch (IOException | InterruptedException e) {
            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            throw new ExchangeApiException("KUCOIN_FUTURES", path, null, null,
                    "network error: " + e.getMessage(), null, e);
        }

        String rawBody = response.body();

        if (response.statusCode() != 200) {
            throw new ExchangeApiException("KUCOIN_FUTURES", path, response.statusCode(), null,
                    "unexpected HTTP status", rawBody, null);
        }

        JsonObject result;
        try {
            result = JsonParser.parseString(rawBody).getAsJsonObject();
        } catch (Exception e) {
            throw new ExchangeApiException("KUCOIN_FUTURES", path, response.statusCode(), null,
                    "response is not valid JSON", rawBody, e);
        }

        System.out.println("[KuCoin Futures] " + path + " -> " + result);

        if (!result.has("code")) {
            throw new ExchangeApiException("KUCOIN_FUTURES", path, response.statusCode(), null,
                    "response has no code field", rawBody, null);
        }

        String code = result.get("code").getAsString();
        if (!SUCCESS_CODE.equals(code)) {
            String msg = result.has("msg") ? result.get("msg").getAsString() : null;
            throw new ExchangeApiException("KUCOIN_FUTURES", path, response.statusCode(), code, msg, rawBody, null);
        }

        return result;
    }
}
