package com.MySelfAlgoWithLoan.prod.calc;

public class InsufficientOrderBookDepthException extends RuntimeException {

    public InsufficientOrderBookDepthException(double requestedQty, double availableQty) {
        super("В стакане недостаточно глубины: запрошено " + requestedQty + ", доступно только " + availableQty);
    }
}