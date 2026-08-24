package com.inditex.similarproducts.product.infrastructure.input.rest;

import com.inditex.similarproducts.product.application.port.in.GetSimilarProductsUseCase;
import com.inditex.similarproducts.product.domain.ProductId;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/product")
final class SimilarProductsController {

    private final GetSimilarProductsUseCase useCase;

    SimilarProductsController(GetSimilarProductsUseCase useCase) {
        this.useCase = useCase;
    }

    @GetMapping("/{productId}/similar")
    List<SimilarProductResponse> getSimilarProducts(@PathVariable String productId) {
        return useCase.getSimilarProducts(new ProductId(productId)).stream()
                .map(SimilarProductResponse::from)
                .toList();
    }
}
