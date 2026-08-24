package com.inditex.similarproducts.product.application.exception;

public final class ExternalServiceTimeoutException extends ExternalServiceException {

    public ExternalServiceTimeoutException(String message, Throwable cause) {
        super(message, cause);
    }
}
