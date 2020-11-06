package com.example.orderservice.order;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.context.annotation.Bean;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.jms.config.AbstractJmsListenerContainerFactory;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.jms.support.converter.MappingJackson2MessageConverter;
import org.springframework.jms.support.converter.MessageType;
import org.springframework.stereotype.Service;

import java.util.function.Consumer;

@Service
public class ShippingService {

    private static final Logger log = LoggerFactory.getLogger(ShippingService.class);

    private final JmsTemplate jmsTemplate;
    private final OrderConfigurationProperties orderConfigurationProperties;
    private Consumer<OrderStatusUpdate> orderStatusUpdateConsumer;

    ShippingService(JmsTemplate jmsTemplate, OrderConfigurationProperties orderConfigurationProperties) {
        this.jmsTemplate = jmsTemplate;
        this.orderConfigurationProperties = orderConfigurationProperties;
    }

    void shipOrder(Order order) {
        if (StringUtils.isEmpty(orderConfigurationProperties.getShippingQueueName())) {
            throw new RuntimeException("order.shipping-queue-name not set");
        }
        jmsTemplate.convertAndSend(orderConfigurationProperties.getShippingQueueName(), order, postProcessor -> {
            // The shipping-service based on Spring Cloud Stream will forward it automatically from the input to the output of the java.util.function.Function
            // postProcessor.setJMSType didn't work
            postProcessor.setStringProperty("_type", OrderStatusUpdate.class.getCanonicalName());
            return postProcessor;
        });
    }

    @JmsListener(destination = "#{@orderConfigurationProperties.deliveredQueueName}",
            containerFactory = "jmsListenerContainerFactory")
    private void updateStatus(OrderStatusUpdate statusUpdate) {
        log.info("updateStatus called for order id: " + statusUpdate.getId() + " with status "
                + statusUpdate.getStatus());
        if (orderStatusUpdateConsumer != null) {
            orderStatusUpdateConsumer.accept(statusUpdate);
        }
    }

    void setOrderStatusUpdateConsumer(Consumer<OrderStatusUpdate> orderStatusUpdateConsumer) {
        this.orderStatusUpdateConsumer = orderStatusUpdateConsumer;
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
                    ((AbstractJmsListenerContainerFactory)bean).setMessageConverter(jacksonJmsMessageConverter);
                }
                return bean;
            }
        };
    }
}
