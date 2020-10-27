package com.example.orderservice.order;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@ConfigurationProperties("order")
@Component
public class OrderConfigurationProperties {

    private String deliveredQueueName;
    private String shippingQueueName;
    private String productsApiUrl;

    private OrderConfigurationProperties() {
    }

    public String getDeliveredQueueName() {
        return deliveredQueueName;
    }

    public void setDeliveredQueueName(String deliveredQueueName) {
        this.deliveredQueueName = deliveredQueueName;
    }

    public String getShippingQueueName() {
        return shippingQueueName;
    }

    public void setShippingQueueName(String shippingQueueName) {
        this.shippingQueueName = shippingQueueName;
    }

    public String getProductsApiUrl() {
        return productsApiUrl;
    }

    public void setProductsApiUrl(String productsApiUrl) {
        this.productsApiUrl = productsApiUrl;
    }
}
