package com.yabozkurt.n11bootcamp.ecommerce.product.infrastructure.messaging.publisher;

import com.yabozkurt.n11bootcamp.ecommerce.product.infrastructure.messaging.event.ProductCreatedEvent;
import com.yabozkurt.n11bootcamp.ecommerce.product.infrastructure.messaging.event.VariantCreatedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class ProductEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(ProductEventPublisher.class);

    private final RabbitTemplate rabbitTemplate;

    @Value("${rabbitmq.exchange.products:product.events}")
    private String productsExchange;

    public ProductEventPublisher(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    public void publishProductCreated(ProductCreatedEvent event) {
        log.info("Publishing product.created event: productId={}", event.getProductId());
        rabbitTemplate.convertAndSend(productsExchange, "product.created", event);
    }

    public void publishVariantCreated(VariantCreatedEvent event) {
        log.info("Publishing variant.created event: productId={} variantId={}", event.getProductId(), event.getVariantId());
        rabbitTemplate.convertAndSend(productsExchange, "variant.created", event);
    }
}
