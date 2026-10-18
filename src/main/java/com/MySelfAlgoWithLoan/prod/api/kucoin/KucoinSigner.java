package com.MySelfAlgoWithLoan.prod.api.kucoin;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.codec.digest.HmacAlgorithms;
import org.apache.commons.codec.digest.HmacUtils;
import org.springframework.stereotype.Component;

@Component
public class KucoinSigner {

    public String sign(String secret, String payload) {
        byte[] raw = new HmacUtils(HmacAlgorithms.HMAC_SHA_256, secret).hmac(payload);
        return Base64.encodeBase64String(raw);
    }

    public String signPassphrase(String secret, String passphrase) {
        return sign(secret, passphrase);
    }
}