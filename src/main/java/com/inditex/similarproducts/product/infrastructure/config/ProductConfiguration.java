package com.inditex.similarproducts.product.infrastructure.config;

import com.inditex.similarproducts.product.application.GetSimilarProductsService;
import com.inditex.similarproducts.product.application.port.in.GetSimilarProductsUseCase;
import com.inditex.similarproducts.product.application.port.out.GetProductDetailPort;
import com.inditex.similarproducts.product.application.port.out.GetSimilarProductIdsPort;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.net.http.HttpClient;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Configuration
class ProductConfiguration {

    @Bean
    RestClient existingProductsRestClient(ExistingApiProperties properties) {
        HttpClient httpClient = HttpClient.newBuilder()
                .connectTimeout(properties.connectTimeout())
                .build();
        JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(httpClient);
        requestFactory.setReadTimeout(properties.readTimeout());

        return RestClient.builder()
                .baseUrl(properties.baseUrl().toString())
                .requestFactory(requestFactory)
                .build();
    }

    @Bean(destroyMethod = "close")
    ExecutorService productDetailExecutor() {
        return Executors.newVirtualThreadPerTaskExecutor();
    }

    @Bean
    GetSimilarProductsUseCase getSimilarProductsUseCase(
            GetSimilarProductIdsPort similarProductIdsPort,
            GetProductDetailPort productDetailPort,
            ExecutorService productDetailExecutor) {
        return new GetSimilarProductsService(
                similarProductIdsPort,
                productDetailPort,
                productDetailExecutor
        );
    }
}
