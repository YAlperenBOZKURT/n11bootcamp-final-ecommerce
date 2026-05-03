package com.yabozkurt.n11bootcamp.ecommerce.cart.infrastructure.messaging.listener;

import com.yabozkurt.n11bootcamp.ecommerce.cart.application.service.CartService;
import com.yabozkurt.n11bootcamp.ecommerce.cart.infrastructure.messaging.event.OrderConfirmedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
public class CartEventListener {

    private static final Logger log = LoggerFactory.getLogger(CartEventListener.class);

    private final CartService cartService;

    public CartEventListener(CartService cartService) {
        this.cartService = cartService;
    }

    @RabbitListener(queues = "${rabbitmq.queues.order-confirmed:cart.order.confirmed}")
    public void onOrderConfirmed(OrderConfirmedEvent event) {
        log.info("Order confirmed, clearing cart for userId={}", event.getUserId());
        try {
            cartService.clearCart(event.getUserId());
        } catch (Exception e) {
            log.warn("Failed to clear cart for userId={}: {}", event.getUserId(), e.getMessage());
        }
    }
}
