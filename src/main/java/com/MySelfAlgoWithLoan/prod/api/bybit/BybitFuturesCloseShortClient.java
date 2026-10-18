package com.MySelfAlgoWithLoan.prod.api.bybit;

import com.MySelfAlgoWithLoan.prod.api.QuantityRounder;
import com.google.gson.JsonObject;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
@RequiredArgsConstructor
public class BybitFuturesCloseShortClient {

    private final BybitRestClient restClient;
    private final BybitInstrumentPrecisionClient precisionClient;

    public JsonObject closeShort(String symbol, BigDecimal qty) {
        BigDecimal step = precisionClient.getBaseQtyStep(symbol, "linear");
        BigDecimal roundedQty = QuantityRounder.roundDown(qty, step);

        JsonObject body = new JsonObject();
        body.addProperty("category", "linear");
        body.addProperty("symbol", symbol);
        body.addProperty("side", "Buy");
        body.addProperty("orderType", "Market");
        body.addProperty("qty", QuantityRounder.toOrderString(roundedQty));
        body.addProperty("positionIdx", 0);
        body.addProperty("reduceOnly", true);
        return restClient.post("/v5/order/create", body);
    }
}