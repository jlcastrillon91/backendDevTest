package com.inditex.similarproducts.product.infrastructure.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.net.URI;
import java.time.Duration;

@ConfigurationProperties("clients.product-api")
public record ExistingApiProperties(URI baseUrl, Duration connectTimeout, Duration readTimeout) {
}
