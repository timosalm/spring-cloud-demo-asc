package com.example.orderservice;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.context.annotation.Bean;
import org.springframework.jms.config.AbstractJmsListenerContainerFactory;
import org.springframework.jms.connection.CachingConnectionFactory;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.jms.support.converter.MappingJackson2MessageConverter;
import org.springframework.jms.support.converter.MessageType;
import org.springframework.web.client.RestTemplate;

@EnableCaching
@EnableDiscoveryClient
@SpringBootApplication
public class OrderServiceApplication {

    @LoadBalanced
    @Bean
    RestTemplate restTemplate(RestTemplateBuilder restTemplateBuilder) {
        return restTemplateBuilder.build();
    }

    @Bean
    MappingJackson2MessageConverter jacksonJmsMessageConverter() {
        final MappingJackson2MessageConverter converter = new MappingJackson2MessageConverter();
        converter.setTargetType(MessageType.TEXT);
        converter.setTypeIdPropertyName("_type");
        return converter;
    }

    @Bean
    BeanPostProcessor applyJacksonJmsMessageConverterMessageConverter(MappingJackson2MessageConverter jacksonJmsMessageConverter) {
        return new BeanPostProcessor() {
            @Override
            public Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException {
                return bean;
            }

            @Override
            public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
                if (bean instanceof JmsTemplate) {
                    ((JmsTemplate) bean).setMessageConverter(jacksonJmsMessageConverter);
                } else if (bean instanceof AbstractJmsListenerContainerFactory) {
                    ((AbstractJmsListenerContainerFactory) bean).setMessageConverter(jacksonJmsMessageConverter);
                } else if (bean instanceof CachingConnectionFactory) {
                    // Fixes link is closed error, see https://docs.microsoft.com/en-us/azure/service-bus-messaging/service-bus-amqp-troubleshoot
                    ((CachingConnectionFactory) bean).setCacheProducers(false);
                }
                return bean;
            }
        };
    }

    public static void main(String[] args) {
        SpringApplication.run(OrderServiceApplication.class, args);
    }
}
