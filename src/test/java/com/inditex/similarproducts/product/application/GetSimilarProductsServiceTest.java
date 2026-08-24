package com.inditex.similarproducts.product.application;

import com.inditex.similarproducts.product.application.port.out.GetProductDetailPort;
import com.inditex.similarproducts.product.domain.Product;
import com.inditex.similarproducts.product.domain.ProductId;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Get similar products")
class GetSimilarProductsServiceTest {

    private final ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();

    @AfterEach
    void closeExecutor() {
        executor.close();
    }

    @Test
    @DisplayName("Returns product details in similarity order")
    void returnsProductDetailsInSimilarityOrder() {
        GetProductDetailPort details = id -> product(id.value());
        var service = new GetSimilarProductsService(
                ignored -> List.of(new ProductId("3"), new ProductId("2"), new ProductId("4")),
                details,
                executor
        );

        List<Product> result = service.getSimilarProducts(new ProductId("1"));

        assertThat(result).extracting(product -> product.id().value())
                .containsExactly("3", "2", "4");
    }

    @Test
    @DisplayName("Returns an empty list when no similar products exist")
    void returnsEmptyListWhenThereAreNoSimilarProducts() {
        var service = new GetSimilarProductsService(
                ignored -> List.of(),
                ignored -> { throw new AssertionError("Details must not be requested"); },
                executor
        );

        assertThat(service.getSimilarProducts(new ProductId("1"))).isEmpty();
    }

    @Test
    @DisplayName("Removes duplicate IDs while preserving similarity order")
    void removesDuplicateIdsWhilePreservingOrder() {
        var service = new GetSimilarProductsService(
                ignored -> List.of(new ProductId("2"), new ProductId("2"), new ProductId("3")),
                id -> product(id.value()),
                executor
        );

        assertThat(service.getSimilarProducts(new ProductId("1")))
                .extracting(product -> product.id().value())
                .containsExactly("2", "3");
    }

    private Product product(String id) {
        return new Product(new ProductId(id), "Product " + id, BigDecimal.TEN, true);
    }
}
