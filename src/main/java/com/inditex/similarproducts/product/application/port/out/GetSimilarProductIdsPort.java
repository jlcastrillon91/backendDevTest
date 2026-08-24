package com.inditex.similarproducts.product.application.port.out;

import com.inditex.similarproducts.product.domain.ProductId;

import java.util.List;

public interface GetSimilarProductIdsPort {

    List<ProductId> getSimilarProductIds(ProductId productId);
}
