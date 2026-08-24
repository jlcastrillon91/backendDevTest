package com.inditex.similarproducts.product.application.port.in;

import com.inditex.similarproducts.product.domain.Product;
import com.inditex.similarproducts.product.domain.ProductId;

import java.util.List;

public interface GetSimilarProductsUseCase {

    List<Product> getSimilarProducts(ProductId productId);
}
