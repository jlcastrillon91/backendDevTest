package com.inditex.similarproducts.product.infrastructure.input.rest;

import com.inditex.similarproducts.product.domain.Product;
import java.math.BigDecimal;

record SimilarProductResponse(
        String id,
        String name,
        BigDecimal price,
        boolean availability) {

    static SimilarProductResponse from(Product product) {
        return new SimilarProductResponse(
                product.id().value(),
                product.name(),
                product.price(),
                product.availability()
        );
    }
}
