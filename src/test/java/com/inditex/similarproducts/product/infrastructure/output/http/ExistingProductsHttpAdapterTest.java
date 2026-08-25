package com.inditex.similarproducts.product.infrastructure.output.http;

import com.inditex.similarproducts.product.application.exception.ExternalServiceException;
import com.inditex.similarproducts.product.application.exception.ExternalServiceTimeoutException;
import com.inditex.similarproducts.product.application.exception.InvalidExternalProductException;
import com.inditex.similarproducts.product.application.exception.ProductNotFoundException;
import com.inditex.similarproducts.product.domain.Product;
import com.inditex.similarproducts.product.domain.ProductId;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.http.HttpClient;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@DisplayName("Existing Products HTTP adapter")
class ExistingProductsHttpAdapterTest {

    private final Map<String, StubResponse> stubs = new ConcurrentHashMap<>();
    private final ExecutorService serverExecutor = Executors.newCachedThreadPool();
    private HttpServer server;
    private ExistingProductsHttpAdapter adapter;

    @BeforeAll
    void startServer() throws IOException {
        server = HttpServer.create(new InetSocketAddress(0), 0);
        server.createContext("/", this::respond);
        server.setExecutor(serverExecutor);
        server.start();
    }

    @AfterAll
    void stopServer() {
        server.stop(0);
        serverExecutor.close();
    }

    @BeforeEach
    void prepareTest() {
        stubs.clear();
        var requestFactory = new JdkClientHttpRequestFactory(HttpClient.newHttpClient());
        requestFactory.setReadTimeout(Duration.ofMillis(100));
        RestClient restClient = RestClient.builder()
                .baseUrl("http://localhost:" + server.getAddress().getPort())
                .requestFactory(requestFactory)
                .build();
        adapter = new ExistingProductsHttpAdapter(restClient);
    }

    @Test
    @DisplayName("Converts string and numeric similar IDs")
    void convertsStringAndNumericSimilarIds() {
        stub("/product/1/similarids", 200, "[\"2\",3]");

        List<ProductId> result = adapter.getSimilarProductIds(new ProductId("1"));

        assertThat(result).extracting(ProductId::value).containsExactly("2", "3");
    }

    @ParameterizedTest(name = "Rejects unsupported similar ID response: {0}")
    @ValueSource(strings = {"[null]", "[true]", "[{}]"})
    @DisplayName("Rejects unsupported similar ID types")
    void rejectsUnsupportedSimilarIdTypes(String response) {
        stub("/product/1/similarids", 200, response);

        assertThatThrownBy(() -> adapter.getSimilarProductIds(new ProductId("1")))
                .isInstanceOf(InvalidExternalProductException.class)
                .hasMessage("Similar product ID must be a string or a number");
    }

    @Test
    @DisplayName("Rejects a blank similar ID")
    void rejectsBlankSimilarId() {
        stub("/product/1/similarids", 200, "[\" \" ]");

        assertThatThrownBy(() -> adapter.getSimilarProductIds(new ProductId("1")))
                .isInstanceOf(InvalidExternalProductException.class)
                .hasMessageContaining("Similar product ID is invalid");
    }

    @Test
    @DisplayName("Maps a valid external product to the domain")
    void mapsValidExternalProductToDomain() {
        stub("/product/2", 200, productJson("2", "Dress", "19.99", true));

        Product result = adapter.getProductDetail(new ProductId("2"));

        assertThat(result.id().value()).isEqualTo("2");
        assertThat(result.name()).isEqualTo("Dress");
        assertThat(result.price()).isEqualByComparingTo("19.99");
        assertThat(result.availability()).isTrue();
    }

    @Test
    @DisplayName("Rejects an external product without a price")
    void rejectsExternalProductWithoutPrice() {
        stub("/product/2", 200, "{\"id\":\"2\",\"name\":\"Dress\",\"availability\":true}");

        assertThatThrownBy(() -> adapter.getProductDetail(new ProductId("2")))
                .isInstanceOf(InvalidExternalProductException.class)
                .hasMessageContaining("Product price is required");
    }

    @Test
    @DisplayName("Rejects an external product without availability")
    void rejectsExternalProductWithoutAvailability() {
        stub("/product/2", 200, "{\"id\":\"2\",\"name\":\"Dress\",\"price\":19.99}");

        assertThatThrownBy(() -> adapter.getProductDetail(new ProductId("2")))
                .isInstanceOf(InvalidExternalProductException.class)
                .hasMessage("External product '2' has no availability");
    }

    @Test
    @DisplayName("Rejects an external product with a different ID")
    void rejectsExternalProductWithDifferentId() {
        stub("/product/2", 200, productJson("3", "Dress", "19.99", true));

        assertThatThrownBy(() -> adapter.getProductDetail(new ProductId("2")))
                .isInstanceOf(InvalidExternalProductException.class)
                .hasMessage("External product ID does not match the requested ID");
    }

    @Test
    @DisplayName("Translates HTTP 404 to product not found")
    void translatesNotFoundResponse() {
        stub("/product/2", 404, "{}");

        assertThatThrownBy(() -> adapter.getProductDetail(new ProductId("2")))
                .isInstanceOf(ProductNotFoundException.class)
                .hasMessage("Product '2' was not found");
    }

    @Test
    @DisplayName("Translates HTTP 5xx to an external service error")
    void translatesServerErrorResponse() {
        stub("/product/2", 500, "{}");

        assertThatThrownBy(() -> adapter.getProductDetail(new ProductId("2")))
                .isInstanceOf(ExternalServiceException.class)
                .hasMessage("Product API returned HTTP 500");
    }

    @Test
    @DisplayName("Translates a read timeout to an external timeout error")
    void translatesReadTimeout() {
        stubs.put("/product/2", new StubResponse(200, productJson("2", "Dress", "19.99", true), 300));

        assertThatThrownBy(() -> adapter.getProductDetail(new ProductId("2")))
                .isInstanceOf(ExternalServiceTimeoutException.class)
                .hasMessage("Product API request timed out");
    }

    private void stub(String path, int status, String body) {
        stubs.put(path, new StubResponse(status, body, 0));
    }

    private String productJson(String id, String name, String price, boolean availability) {
        return "{\"id\":\"%s\",\"name\":\"%s\",\"price\":%s,\"availability\":%s}"
                .formatted(id, name, price, availability);
    }

    private void respond(HttpExchange exchange) throws IOException {
        StubResponse response = stubs.getOrDefault(exchange.getRequestURI().getPath(), new StubResponse(404, "{}", 0));
        if (response.delayMillis() > 0) {
            try {
                Thread.sleep(response.delayMillis());
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
            }
        }
        byte[] body = response.body().getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json");
        exchange.sendResponseHeaders(response.status(), body.length);
        exchange.getResponseBody().write(body);
        exchange.close();
    }

    private record StubResponse(int status, String body, long delayMillis) {
    }
}
