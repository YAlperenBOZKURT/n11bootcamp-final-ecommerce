package com.yabozkurt.n11bootcamp.ecommerce.order.infrastructure.messaging.publisher;

import com.yabozkurt.n11bootcamp.ecommerce.order.infrastructure.messaging.event.OrderCancelledEvent;
import com.yabozkurt.n11bootcamp.ecommerce.order.infrastructure.messaging.event.OrderConfirmedEvent;
import com.yabozkurt.n11bootcamp.ecommerce.order.infrastructure.messaging.event.OrderFailedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class OrderEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(OrderEventPublisher.class);

    private final RabbitTemplate rabbitTemplate;

    @Value("${rabbitmq.exchange.orders:order.events}")
    private String ordersExchange;

    public OrderEventPublisher(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    public void publishOrderConfirmed(OrderConfirmedEvent event) {
        publishSafely("order.confirmed", event.getOrderId(), event);
    }

    public void publishOrderFailed(OrderFailedEvent event) {
        publishSafely("order.failed", event.getOrderId(), event);
    }

    public void publishOrderCancelled(OrderCancelledEvent event) {
        publishSafely("order.cancelled", event.getOrderId(), event);
    }

    private void publishSafely(String routingKey, String orderId, Object payload) {
        try {
            log.info("Publishing {} event for orderId={}", routingKey, orderId);
            rabbitTemplate.convertAndSend(ordersExchange, routingKey, payload);
        } catch (Exception ex) {
            // Order response should not fail after DB state is already finalized
            log.error("Failed to publish {} event for orderId={}: {}", routingKey, orderId, ex.getMessage());
        }
    }
}
