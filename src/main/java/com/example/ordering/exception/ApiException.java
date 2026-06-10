package com.example.ordering.exception;

public class ApiException extends RuntimeException {
    private final int httpStatus;
    private final String code;

    public ApiException(final int httpStatus, final String code, final String message) {
        super(message);
        this.httpStatus = httpStatus;
        this.code = code;
    }

    public int getHttpStatus() {
        return httpStatus;
    }

    public String getCode() {
        return code;
    }
}
