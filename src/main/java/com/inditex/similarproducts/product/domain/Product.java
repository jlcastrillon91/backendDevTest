package com.inditex.similarproducts.product.domain;

import com.inditex.similarproducts.product.domain.exception.InvalidProductException;

import java.math.BigDecimal;

public record Product(ProductId id, String name, BigDecimal price, boolean availability) {

    public Product {
        if (id == null) {
            throw new InvalidProductException("Product ID is required");
        }
        if (name == null || name.isBlank()) {
            throw new InvalidProductException("Product name is required");
        }
        if (price == null) {
            throw new InvalidProductException("Product price is required");
        }
    }
}
