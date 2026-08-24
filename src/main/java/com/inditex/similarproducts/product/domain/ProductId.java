package com.inditex.similarproducts.product.domain;

import com.inditex.similarproducts.product.domain.exception.InvalidProductException;

public record ProductId(String value) {

    public ProductId {
        if (value == null || value.isBlank()) {
            throw new InvalidProductException("Product ID must not be blank");
        }
    }
}
