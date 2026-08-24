package com.inditex.similarproducts.product.infrastructure.input.rest;

import com.inditex.similarproducts.product.application.exception.ExternalServiceException;
import com.inditex.similarproducts.product.application.exception.ExternalServiceTimeoutException;
import com.inditex.similarproducts.product.application.exception.InvalidExternalProductException;
import com.inditex.similarproducts.product.application.exception.ProductNotFoundException;
import com.inditex.similarproducts.product.domain.exception.InvalidProductException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
final class ApiExceptionHandler {

    @ExceptionHandler(InvalidProductException.class)
    ResponseEntity<ApiError> handleInvalidProduct(InvalidProductException exception) {
        return response(HttpStatus.BAD_REQUEST, "INVALID_PRODUCT", exception);
    }

    @ExceptionHandler(ProductNotFoundException.class)
    ResponseEntity<ApiError> handleNotFound(ProductNotFoundException exception) {
        return response(HttpStatus.NOT_FOUND, "PRODUCT_NOT_FOUND", exception);
    }

    @ExceptionHandler(ExternalServiceTimeoutException.class)
    ResponseEntity<ApiError> handleTimeout(ExternalServiceTimeoutException exception) {
        return response(HttpStatus.GATEWAY_TIMEOUT, "PRODUCT_API_TIMEOUT", exception);
    }

    @ExceptionHandler(InvalidExternalProductException.class)
    ResponseEntity<ApiError> handleInvalidExternalProduct(InvalidExternalProductException exception) {
        return response(HttpStatus.BAD_GATEWAY, "INVALID_EXTERNAL_PRODUCT", exception);
    }

    @ExceptionHandler(ExternalServiceException.class)
    ResponseEntity<ApiError> handleExternalService(ExternalServiceException exception) {
        return response(HttpStatus.BAD_GATEWAY, "PRODUCT_API_ERROR", exception);
    }

    private ResponseEntity<ApiError> response(HttpStatus status, String code, RuntimeException exception) {
        return ResponseEntity.status(status).body(new ApiError(code, exception.getMessage()));
    }
}
