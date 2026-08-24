package com.inditex.similarproducts.product.application;

import com.inditex.similarproducts.product.application.exception.ExternalServiceException;
import com.inditex.similarproducts.product.application.port.in.GetSimilarProductsUseCase;
import com.inditex.similarproducts.product.application.port.out.GetProductDetailPort;
import com.inditex.similarproducts.product.application.port.out.GetSimilarProductIdsPort;
import com.inditex.similarproducts.product.domain.Product;
import com.inditex.similarproducts.product.domain.ProductId;

import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

public final class GetSimilarProductsService implements GetSimilarProductsUseCase {

    private final GetSimilarProductIdsPort similarProductIdsPort;
    private final GetProductDetailPort productDetailPort;
    private final ExecutorService executor;

    public GetSimilarProductsService(GetSimilarProductIdsPort similarProductIdsPort,
                                     GetProductDetailPort productDetailPort,
                                     ExecutorService executor) {
        this.similarProductIdsPort = similarProductIdsPort;
        this.productDetailPort = productDetailPort;
        this.executor = executor;
    }

    @Override
    public List<Product> getSimilarProducts(ProductId productId) {
        List<ProductId> ids = similarProductIdsPort.getSimilarProductIds(productId);
        List<Future<Product>> detailRequests = ids.stream()
                .distinct()
                .map(id -> executor.submit(() -> productDetailPort.getProductDetail(id)))
                .toList();

        return detailRequests.stream().map(this::await).toList();
    }

    private Product await(Future<Product> request) {
        try {
            return request.get();
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new ExternalServiceException("Product detail request was interrupted", exception);
        } catch (ExecutionException exception) {
            if (exception.getCause() instanceof RuntimeException runtimeException) {
                throw runtimeException;
            }
            throw new ExternalServiceException("Could not obtain product detail", exception.getCause());
        }
    }
}
