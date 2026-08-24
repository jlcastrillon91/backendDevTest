package com.inditex.similarproducts.product.application.exception;

public final class ProductNotFoundException extends RuntimeException {

    public ProductNotFoundException(String productId) {
        super("Product '%s' was not found".formatted(productId));
    }
}
