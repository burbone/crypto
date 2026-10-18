package com.MySelfAlgoWithLoan.prod.util;

public final class SymbolFormat {

    private static final String QUOTE = "USDT";

    private SymbolFormat() {
    }

    public static String toKucoinSymbol(String bybitSymbol) {
        return coinFromSymbol(bybitSymbol) + "-" + QUOTE;
    }

    public static String coinFromSymbol(String bybitSymbol) {
        if (!bybitSymbol.endsWith(QUOTE)) {
            throw new IllegalArgumentException("Пара не оканчивается на " + QUOTE + ": " + bybitSymbol);
        }
        return bybitSymbol.substring(0, bybitSymbol.length() - QUOTE.length());
    }
}