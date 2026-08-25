package com.inditex.similarproducts.product.e2e;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import io.restassured.response.Response;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static io.restassured.RestAssured.given;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@DisplayName("Similar Products API end-to-end")
class SimilarProductsE2ETest {

    private static final Map<String, StubResponse> STUBS = new ConcurrentHashMap<>();
    private static final ExecutorService STUB_EXECUTOR = Executors.newCachedThreadPool();
    private static final HttpServer PRODUCT_API = startProductApi();
    private static final AtomicInteger ACTIVE_REQUESTS = new AtomicInteger();
    private static final AtomicInteger MAX_CONCURRENT_REQUESTS = new AtomicInteger();

    @LocalServerPort
    private int applicationPort;

    private static HttpServer startProductApi() {
        try {
            HttpServer server = HttpServer.create(new InetSocketAddress(0), 0);
            server.createContext("/", SimilarProductsE2ETest::respond);
            server.setExecutor(STUB_EXECUTOR);
            server.start();
            return server;
        } catch (IOException exception) {
            throw new ExceptionInInitializerError(exception);
        }
    }

    @AfterAll
    static void stopProductApi() {
        PRODUCT_API.stop(0);
        STUB_EXECUTOR.close();
    }

    @DynamicPropertySource
    static void externalApiProperties(DynamicPropertyRegistry registry) {
        registry.add("clients.product-api.base-url", () -> "http://localhost:" + PRODUCT_API.getAddress().getPort());
        registry.add("clients.product-api.connect-timeout", () -> "200ms");
        registry.add("clients.product-api.read-timeout", () -> "500ms");
    }

    @BeforeEach
    void clearStubs() {
        STUBS.clear();
        ACTIVE_REQUESTS.set(0);
        MAX_CONCURRENT_REQUESTS.set(0);
    }

    @Test
    @DisplayName("Returns product details in similarity order and accepts numeric external IDs")
    void returnsSimilarProductsInContractOrderAndAcceptsNumericMockIds() {
        stub("/product/1/similarids", 200, "[2,3,4]");
        stubProduct("2", "Dress", "19.99", true);
        stubProduct("3", "Blazer", "29.99", false);
        stubProduct("4", "Boots", "39.99", true);

        Response response = get("/product/1/similar");

        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.asString()).isEqualTo("[{\"id\":\"2\",\"name\":\"Dress\",\"price\":19.99,\"availability\":true},"
                + "{\"id\":\"3\",\"name\":\"Blazer\",\"price\":29.99,\"availability\":false},"
                + "{\"id\":\"4\",\"name\":\"Boots\",\"price\":39.99,\"availability\":true}]");
    }

    @Test
    @DisplayName("Returns 200 with an empty array when no similar products exist")
    void returnsEmptyArrayWhenThereAreNoSimilarProducts() {
        stub("/product/1/similarids", 200, "[]");

        Response response = get("/product/1/similar");

        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.asString()).isEqualTo("[]");
    }

    @Test
    @DisplayName("Returns 404 when the requested product does not exist")
    void returnsNotFoundWhenProductDoesNotExist() {
        stub("/product/unknown/similarids", 404, "{\"message\":\"Product not found\"}");

        Response response = get("/product/unknown/similar");

        assertThat(response.statusCode()).isEqualTo(404);
        assertThat(response.asString()).contains("PRODUCT_NOT_FOUND");
    }

    @Test
    @DisplayName("Returns 502 when an external product has no price")
    void returnsBadGatewayWhenExternalProductHasNoPrice() {
        stub("/product/1/similarids", 200, "[\"2\"]");
        stub("/product/2", 200, "{\"id\":\"2\",\"name\":\"Dress\",\"availability\":true}");

        Response response = get("/product/1/similar");

        assertThat(response.statusCode()).isEqualTo(502);
        assertThat(response.asString()).contains("INVALID_EXTERNAL_PRODUCT", "Product price is required");
    }

    @Test
    @DisplayName("Returns 502 when the Products API fails")
    void returnsBadGatewayWhenProductApiFails() {
        stub("/product/1/similarids", 500, "{}");

        Response response = get("/product/1/similar");

        assertThat(response.statusCode()).isEqualTo(502);
        assertThat(response.asString()).contains("PRODUCT_API_ERROR");
    }

    @Test
    @DisplayName("Returns 504 when the Products API times out")
    void returnsGatewayTimeoutWhenProductApiIsTooSlow() {
        STUBS.put("/product/1/similarids", new StubResponse(200, "[]", 800));

        Response response = get("/product/1/similar");

        assertThat(response.statusCode()).isEqualTo(504);
        assertThat(response.asString()).contains("PRODUCT_API_TIMEOUT");
    }

    @Test
    @DisplayName("Returns 400 for a blank product ID")
    void returnsBadRequestForBlankProductId() {
        Response response = get("/product/%20/similar");

        assertThat(response.statusCode()).isEqualTo(400);
        assertThat(response.asString()).contains("INVALID_PRODUCT");
    }

    @Test
    @DisplayName("Loads product details concurrently without changing their order")
    void loadsProductDetailsConcurrentlyAndStillPreservesOrder() {
        stub("/product/1/similarids", 200, "[\"2\",\"3\",\"4\"]");
        delayedProduct("2", 300);
        delayedProduct("3", 300);
        delayedProduct("4", 300);

        Response response = get("/product/1/similar");

        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.asString()).containsSubsequence("\"id\":\"2\"", "\"id\":\"3\"", "\"id\":\"4\"");
        assertThat(MAX_CONCURRENT_REQUESTS).hasValueGreaterThanOrEqualTo(2);
    }

    @Test
    @DisplayName("Exposes Swagger UI backed by the API First contract")
    void exposesSwaggerUiUsingTheApiFirstContract() {
        Response contract = get("/openapi/similarProducts.yaml");
        Response swaggerUi = get("/swagger-ui.html");

        assertThat(contract.statusCode()).isEqualTo(200);
        assertThat(contract.asString())
                .contains("title: Similar Products API", "ProductApiTimeout", "ApiError");
        assertThat(swaggerUi.statusCode()).isEqualTo(302);
        assertThat(swaggerUi.header("Location")).contains("/swagger-ui/index.html");
    }

    private Response get(String path) {
        return given()
                .baseUri("http://localhost:" + applicationPort)
                .urlEncodingEnabled(false)
                .redirects().follow(false)
                .when()
                .get(path);
    }

    private static void stubProduct(String id, String name, String price, boolean availability) {
        stub("/product/" + id, 200, "{\"id\":\"%s\",\"name\":\"%s\",\"price\":%s,\"availability\":%s}"
                .formatted(id, name, price, availability));
    }

    private static void delayedProduct(String id, long delayMillis) {
        STUBS.put("/product/" + id, new StubResponse(
                200,
                "{\"id\":\"%s\",\"name\":\"Product %s\",\"price\":10,\"availability\":true}"
                        .formatted(id, id),
                delayMillis
        ));
    }

    private static void stub(String path, int status, String body) {
        STUBS.put(path, new StubResponse(status, body, 0));
    }

    private static void respond(HttpExchange exchange) throws IOException {
        int activeRequests = ACTIVE_REQUESTS.incrementAndGet();
        MAX_CONCURRENT_REQUESTS.accumulateAndGet(activeRequests, Math::max);
        try {
            StubResponse stub = STUBS.getOrDefault(
                    exchange.getRequestURI().getPath(),
                    new StubResponse(404, "{}", 0)
            );
            try {
                Thread.sleep(stub.delayMillis());
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
            }
            byte[] body = stub.body().getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(stub.status(), body.length);
            exchange.getResponseBody().write(body);
        } finally {
            ACTIVE_REQUESTS.decrementAndGet();
            exchange.close();
        }
    }

    private record StubResponse(int status, String body, long delayMillis) {
    }
}
