package com.MySelfAlgoWithLoan.prod.api.kucoin;

import com.google.gson.JsonObject;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class KucoinShortClient {

    private final KucoinFuturesRestClient restClient;
    private final KucoinFuturesInstrumentClient instrumentClient;

    public JsonObject openShort(String spotSymbol, BigDecimal coinQty) {
        KucoinFuturesInstrumentClient.ContractInfo info = instrumentClient.getContractInfo(spotSymbol);

        BigDecimal size = coinQty.divide(info.multiplier(), 0, RoundingMode.FLOOR);
        size = size.divide(info.lotSize(), 0, RoundingMode.FLOOR).multiply(info.lotSize());

        if (size.compareTo(info.lotSize()) < 0) {
            throw new IllegalArgumentException("Size too small for " + spotSymbol);
        }

        JsonObject body = new JsonObject();
        body.addProperty("clientOid", UUID.randomUUID().toString());
        body.addProperty("side", "sell");
        body.addProperty("symbol", info.symbol());
        body.addProperty("type", "market");
        body.addProperty("leverage", 1);
        body.addProperty("size", size.toPlainString());
        body.addProperty("marginMode", "ISOLATED");
        body.addProperty("reduceOnly", false);
        return restClient.post("/api/v1/orders", body);
    }
}