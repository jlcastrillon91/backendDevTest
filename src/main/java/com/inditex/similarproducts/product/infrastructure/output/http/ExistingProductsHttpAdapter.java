package com.inditex.similarproducts.product.infrastructure.output.http;

import com.inditex.similarproducts.product.application.exception.ExternalServiceException;
import com.inditex.similarproducts.product.application.exception.ExternalServiceTimeoutException;
import com.inditex.similarproducts.product.application.exception.InvalidExternalProductException;
import com.inditex.similarproducts.product.application.exception.ProductNotFoundException;
import com.inditex.similarproducts.product.application.port.out.GetProductDetailPort;
import com.inditex.similarproducts.product.application.port.out.GetSimilarProductIdsPort;
import com.inditex.similarproducts.product.domain.Product;
import com.inditex.similarproducts.product.domain.ProductId;
import com.inditex.similarproducts.product.domain.exception.InvalidProductException;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.net.http.HttpTimeoutException;
import java.util.List;

@Component
final class ExistingProductsHttpAdapter implements GetSimilarProductIdsPort, GetProductDetailPort {

    private static final ParameterizedTypeReference<List<Object>> ID_LIST =
            new ParameterizedTypeReference<>() { };

    private final RestClient restClient;

    ExistingProductsHttpAdapter(RestClient existingProductsRestClient) {
        this.restClient = existingProductsRestClient;
    }

    @Override
    public List<ProductId> getSimilarProductIds(ProductId productId) {
        try {
            List<Object> response = restClient.get()
                    .uri("/product/{productId}/similarids", productId.value())
                    .retrieve()
                    .body(ID_LIST);

            if (response == null) {
                throw new InvalidExternalProductException("Similar product IDs response is empty");
            }
            return response.stream()
                    .map(this::toProductId)
                    .toList();
        } catch (RestClientResponseException exception) {
            throw translateHttpError(productId, exception);
        } catch (ResourceAccessException exception) {
            throw translateAccessError(exception);
        }
    }

    private ProductId toProductId(Object value) {
        if (!(value instanceof String) && !(value instanceof Number)) {
            throw new InvalidExternalProductException(
                    "Similar product ID must be a string or a number"
            );
        }
        try {
            return new ProductId(value.toString());
        } catch (InvalidProductException exception) {
            throw new InvalidExternalProductException(
                    "Similar product ID is invalid: " + exception.getMessage()
            );
        }
    }

    @Override
    public Product getProductDetail(ProductId productId) {
        try {
            ProductApiResponse response = restClient.get()
                    .uri("/product/{productId}", productId.value())
                    .retrieve()
                    .body(ProductApiResponse.class);
            return toDomain(response, productId);
        } catch (RestClientResponseException exception) {
            throw translateHttpError(productId, exception);
        } catch (ResourceAccessException exception) {
            throw translateAccessError(exception);
        }
    }

    private Product toDomain(ProductApiResponse response, ProductId requestedId) {
        if (response == null) {
            throw new InvalidExternalProductException("Product response is empty");
        }
        try {
            ProductId responseId = new ProductId(response.id());
            if (!requestedId.equals(responseId)) {
                throw new InvalidExternalProductException(
                        "External product ID does not match the requested ID"
                );
            }
            if (response.availability() == null) {
                throw new InvalidExternalProductException(
                        "External product '%s' has no availability".formatted(response.id())
                );
            }

            return new Product(
                    responseId,
                    response.name(),
                    response.price(),
                    response.availability()
            );
        } catch (InvalidProductException exception) {
            throw new InvalidExternalProductException(
                    "External product '%s' is invalid: %s"
                            .formatted(requestedId.value(), exception.getMessage())
            );
        }
    }

    private RuntimeException translateHttpError(ProductId productId, RestClientResponseException exception) {
        if (exception.getStatusCode().equals(HttpStatus.NOT_FOUND)) {
            return new ProductNotFoundException(productId.value());
        }
        return new ExternalServiceException(
                "Product API returned HTTP %d".formatted(exception.getStatusCode().value()),
                exception
        );
    }

    private RuntimeException translateAccessError(ResourceAccessException exception) {
        if (hasCause(exception, HttpTimeoutException.class)) {
            return new ExternalServiceTimeoutException("Product API request timed out", exception);
        }
        return new ExternalServiceException("Product API is unavailable", exception);
    }

    private boolean hasCause(Throwable throwable, Class<? extends Throwable> type) {
        Throwable current = throwable;
        while (current != null) {
            if (type.isInstance(current)) {
                return true;
            }
            current = current.getCause();
        }
        return false;
    }
}
