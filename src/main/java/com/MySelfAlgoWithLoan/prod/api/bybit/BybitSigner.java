package com.MySelfAlgoWithLoan.prod.api.bybit;

import org.apache.commons.codec.digest.HmacAlgorithms;
import org.apache.commons.codec.digest.HmacUtils;
import org.springframework.stereotype.Component;

@Component
public class BybitSigner {

    public String sign(String secret, String payload) {
        return new HmacUtils(HmacAlgorithms.HMAC_SHA_256, secret).hmacHex(payload);
    }
}