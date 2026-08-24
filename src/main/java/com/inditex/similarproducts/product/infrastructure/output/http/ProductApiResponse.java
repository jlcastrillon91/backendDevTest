package com.inditex.similarproducts.product.infrastructure.output.http;

import java.math.BigDecimal;

record ProductApiResponse(String id, String name, BigDecimal price, Boolean availability) {
}
