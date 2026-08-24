package com.inditex.similarproducts.product.domain;

import com.inditex.similarproducts.product.domain.exception.InvalidProductException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("Product validation")
class ProductTest {

    @Test
    @DisplayName("Rejects a product without an ID")
    void rejectsProductWithoutId() {
        assertThatThrownBy(() -> new Product(null, "Dress", BigDecimal.TEN, true))
                .isInstanceOf(InvalidProductException.class)
                .hasMessage("Product ID is required");
    }

    @Test
    @DisplayName("Rejects a product without a name")
    void rejectsProductWithoutName() {
        assertThatThrownBy(() -> new Product(new ProductId("1"), " ", BigDecimal.TEN, true))
                .isInstanceOf(InvalidProductException.class)
                .hasMessage("Product name is required");
    }

    @Test
    @DisplayName("Rejects a product without a price")
    void rejectsProductWithoutPrice() {
        assertThatThrownBy(() -> new Product(new ProductId("1"), "Dress", null, true))
                .isInstanceOf(InvalidProductException.class)
                .hasMessage("Product price is required");
    }
}
