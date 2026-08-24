package com.inditex.similarproducts.product.application.port.out;

import com.inditex.similarproducts.product.domain.Product;
import com.inditex.similarproducts.product.domain.ProductId;

public interface GetProductDetailPort {

    Product getProductDetail(ProductId productId);
}
