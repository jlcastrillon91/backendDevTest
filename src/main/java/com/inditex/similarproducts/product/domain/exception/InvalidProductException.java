package com.inditex.similarproducts.product.domain.exception;

public final class InvalidProductException extends RuntimeException {

    public InvalidProductException(String message) {
        super(message);
    }
}
