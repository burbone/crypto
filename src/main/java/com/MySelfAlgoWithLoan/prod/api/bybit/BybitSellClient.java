package com.MySelfAlgoWithLoan.prod.api.bybit;

import com.MySelfAlgoWithLoan.prod.api.QuantityRounder;
import com.google.gson.JsonObject;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
@RequiredArgsConstructor
public class BybitSellClient {

    private final BybitRestClient restClient;
    private final BybitInstrumentPrecisionClient precisionClient;

    public JsonObject marketSell(String symbol, BigDecimal qty) {
        BigDecimal step = precisionClient.getBaseQtyStep(symbol);
        BigDecimal roundedQty = QuantityRounder.roundDown(qty, step);

        JsonObject body = new JsonObject();
        body.addProperty("category", "spot");
        body.addProperty("symbol", symbol);
        body.addProperty("side", "Sell");
        body.addProperty("orderType", "Market");
        body.addProperty("qty", QuantityRounder.toOrderString(roundedQty));
        return restClient.post("/v5/order/create", body);
    }
}