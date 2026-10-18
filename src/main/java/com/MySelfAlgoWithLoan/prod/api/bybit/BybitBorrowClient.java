package com.MySelfAlgoWithLoan.prod.api.bybit;

import com.MySelfAlgoWithLoan.prod.api.QuantityRounder;
import com.google.gson.JsonObject;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
@RequiredArgsConstructor
public class BybitBorrowClient {

    private final BybitRestClient restClient;
    private final BybitInstrumentPrecisionClient precisionClient;

    public JsonObject borrow(String coin, BigDecimal qty) {
        BigDecimal step = precisionClient.getBaseQtyStep(coin + "USDT");
        BigDecimal roundedQty = QuantityRounder.roundDown(qty, step);

        JsonObject body = new JsonObject();
        body.addProperty("coin", coin);
        body.addProperty("qty", QuantityRounder.toOrderString(roundedQty));
        return restClient.post("/v5/spot-cross-margin-trade/loan", body);
    }
}