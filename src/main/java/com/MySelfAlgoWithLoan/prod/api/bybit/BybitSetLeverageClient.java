package com.MySelfAlgoWithLoan.prod.api.bybit;

import com.google.gson.JsonObject;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class BybitSetLeverageClient {

    private final BybitRestClient restClient;

    public JsonObject setLeverage(String symbol, String buyLeverage, String sellLeverage) {
        JsonObject body = new JsonObject();
        body.addProperty("category", "linear");
        body.addProperty("symbol", symbol);
        body.addProperty("buyLeverage", buyLeverage);
        body.addProperty("sellLeverage", sellLeverage);
        return restClient.post("/v5/position/set-leverage", body);
    }
}