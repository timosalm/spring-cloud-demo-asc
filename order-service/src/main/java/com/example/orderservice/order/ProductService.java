package com.example.orderservice.order;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cloud.client.circuitbreaker.CircuitBreakerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

@Service
class ProductService {

    private static final Logger log = LoggerFactory.getLogger(ProductService.class);

    private final RestTemplate restTemplate;
    private final CircuitBreakerFactory circuitBreakerFactory;
    private final OrderConfigurationProperties orderConfigurationProperties;

    ProductService(RestTemplate restTemplate, CircuitBreakerFactory circuitBreakerFactory,
                   OrderConfigurationProperties orderConfigurationProperties) {
        this.restTemplate = restTemplate;
        this.circuitBreakerFactory = circuitBreakerFactory;
        this.orderConfigurationProperties = orderConfigurationProperties;
    }

    @Cacheable("Products")
    public List<Product> fetchProducts() {
        if (StringUtils.isEmpty(orderConfigurationProperties.getProductsApiUrl())) {
            throw new RuntimeException("order.products-api-url not set");
        }
        return circuitBreakerFactory.create("products").run(() ->
                        Arrays.asList(Objects.requireNonNull(
                                restTemplate.getForObject(orderConfigurationProperties.getProductsApiUrl(), Product[].class)
                        )),
                throwable -> {
                    log.error("Call to product service failed, using empty product list as fallback", throwable);
                    return Collections.emptyList();
                });
    }
}