package com.MySelfAlgoWithLoan.prod.api.kucoin;

import com.MySelfAlgoWithLoan.prod.api.QuantityRounder;
import com.google.gson.JsonObject;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class KucoinSellClient {

    private static final String ORDERS_PATH = "/api/v1/orders";

    private final KucoinRestClient restClient;
    private final KucoinInstrumentPrecisionClient precisionClient;

    public JsonObject marketSell(String symbol, BigDecimal size) {
        BigDecimal step = precisionClient.getBaseQtyStep(symbol);
        BigDecimal roundedSize = QuantityRounder.roundDown(size, step);

        JsonObject body = new JsonObject();
        body.addProperty("clientOid", UUID.randomUUID().toString());
        body.addProperty("side", "sell");
        body.addProperty("symbol", symbol);
        body.addProperty("type", "market");
        body.addProperty("size", QuantityRounder.toOrderString(roundedSize));
        return restClient.post(ORDERS_PATH, body);
    }
}