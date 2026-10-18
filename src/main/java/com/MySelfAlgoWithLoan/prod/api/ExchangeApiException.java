package com.MySelfAlgoWithLoan.prod.api;

public class ExchangeApiException extends RuntimeException {

    private final String exchange;
    private final String path;
    private final Integer httpStatus;
    private final String apiCode;
    private final String rawBody;

    public ExchangeApiException(String exchange, String path, Integer httpStatus,
                                String apiCode, String message, String rawBody, Throwable cause) {
        super(buildMessage(exchange, path, httpStatus, apiCode, message), cause);
        this.exchange = exchange;
        this.path = path;
        this.httpStatus = httpStatus;
        this.apiCode = apiCode;
        this.rawBody = rawBody;
    }

    private static String buildMessage(String exchange, String path, Integer httpStatus,
                                       String apiCode, String message) {
        return "[" + exchange + "] " + path
                + (httpStatus != null ? " http=" + httpStatus : "")
                + (apiCode != null ? " code=" + apiCode : "")
                + (message != null ? " msg=" + message : "");
    }

    public String getExchange() {
        return exchange;
    }

    public String getPath() {
        return path;
    }

    public Integer getHttpStatus() {
        return httpStatus;
    }

    public String getApiCode() {
        return apiCode;
    }

    public String getRawBody() {
        return rawBody;
    }
}