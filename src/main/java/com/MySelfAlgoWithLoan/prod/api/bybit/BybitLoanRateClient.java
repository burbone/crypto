package com.MySelfAlgoWithLoan.prod.api.bybit;

import com.MySelfAlgoWithLoan.prod.api.ExchangeApiException;
import com.MySelfAlgoWithLoan.prod.config.Properties;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.concurrent.ConcurrentHashMap;

@Component
@RequiredArgsConstructor
public class BybitLoanRateClient {

    private static final String VIP_LEVEL = "No VIP";
    private static final String RECV_WINDOW = "5000";

    private final Properties properties;
    private final BybitSigner signer;

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    private final ConcurrentHashMap<String, CachedRate> cache = new ConcurrentHashMap<>();

    public BigDecimal getPerMinuteBorrowRate(String coin) {
        return getHourlyBorrowRate(coin).divide(BigDecimal.valueOf(60), 12, RoundingMode.HALF_UP);
    }

    public BigDecimal getHourlyBorrowRate(String coin) {
        CachedRate cached = cache.get(coin);
        long ttlMillis = properties.getLoanRateCacheSeconds() * 1000L;

        if (cached != null && System.currentTimeMillis() - cached.fetchedAtMillis() < ttlMillis) {
            return cached.hourlyRate();
        }

        BigDecimal fetched = fetchHourlyBorrowRate(coin);
        cache.put(coin, new CachedRate(fetched, System.currentTimeMillis()));
        return fetched;
    }

    private BigDecimal fetchHourlyBorrowRate(String coin) {
        String path = "/v5/spot-margin-trade/data";

        String signQueryString = "currency=" + coin + "&vipLevel=" + VIP_LEVEL;

        String timestamp = String.valueOf(System.currentTimeMillis());
        String apiKey = properties.getBybit().getApiKey();
        String secret = properties.getBybit().getApiSecret();

        String signPayload = timestamp + apiKey + RECV_WINDOW + signQueryString;
        String signature = signer.sign(secret, signPayload);

        String encodedQueryString = "currency=" + coin
                + "&vipLevel=" + URLEncoder.encode(VIP_LEVEL, StandardCharsets.UTF_8);
        String url = properties.getBybit().getBaseUrl() + path + "?" + encodedQueryString;

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(Duration.ofSeconds(15))
                .header("X-BAPI-API-KEY", apiKey)
                .header("X-BAPI-TIMESTAMP", timestamp)
                .header("X-BAPI-RECV-WINDOW", RECV_WINDOW)
                .header("X-BAPI-SIGN", signature)
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
                    "network error while fetching loan rate: " + e.getMessage(), null, e);
        }

        String rawBody = response.body();
        if (response.statusCode() != 200) {
            throw new ExchangeApiException("BYBIT", path, response.statusCode(), null,
                    "unexpected HTTP status while fetching loan rate", rawBody, null);
        }

        JsonObject result;
        try {
            result = JsonParser.parseString(rawBody).getAsJsonObject();
        } catch (Exception e) {
            throw new ExchangeApiException("BYBIT", path, response.statusCode(), null,
                    "loan-rate response is not valid JSON", rawBody, e);
        }

        int retCode = result.has("retCode") ? result.get("retCode").getAsInt() : -1;
        if (retCode != 0) {
            String retMsg = result.has("retMsg") ? result.get("retMsg").getAsString() : null;
            throw new ExchangeApiException("BYBIT", path, response.statusCode(),
                    String.valueOf(retCode), retMsg, rawBody, null);
        }

        JsonArray vipCoinList = result.getAsJsonObject("result").getAsJsonArray("vipCoinList");
        for (int i = 0; i < vipCoinList.size(); i++) {
            JsonArray list = vipCoinList.get(i).getAsJsonObject().getAsJsonArray("list");
            for (int j = 0; j < list.size(); j++) {
                JsonObject coinEntry = list.get(j).getAsJsonObject();
                if (coin.equals(coinEntry.get("currency").getAsString())) {
                    return new BigDecimal(coinEntry.get("hourlyBorrowRate").getAsString());
                }
            }
        }

        throw new ExchangeApiException("BYBIT", path, response.statusCode(), null,
                "coin " + coin + " not found in loan rate response", rawBody, null);
    }

    private record CachedRate(BigDecimal hourlyRate, long fetchedAtMillis) {
    }
}
