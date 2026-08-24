package com.inditex.similarproducts.product.domain;

import com.inditex.similarproducts.product.domain.exception.InvalidProductException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("Product ID validation")
class ProductIdTest {

    @Test
    @DisplayName("Rejects a blank product ID")
    void rejectsBlankProductId() {
        assertThatThrownBy(() -> new ProductId(" "))
                .isInstanceOf(InvalidProductException.class)
                .hasMessage("Product ID must not be blank");
    }

    @Test
    @DisplayName("Rejects a null product ID")
    void rejectsNullProductId() {
        assertThatThrownBy(() -> new ProductId(null))
                .isInstanceOf(InvalidProductException.class);
    }
}
