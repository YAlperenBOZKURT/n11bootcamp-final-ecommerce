package com.yabozkurt.n11bootcamp.ecommerce.payment.infrastructure.messaging.publisher;

import com.yabozkurt.n11bootcamp.ecommerce.payment.infrastructure.messaging.event.PaymentCompletedEvent;
import com.yabozkurt.n11bootcamp.ecommerce.payment.infrastructure.messaging.event.PaymentFailedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class PaymentEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(PaymentEventPublisher.class);

    private final RabbitTemplate rabbitTemplate;

    @Value("${rabbitmq.exchange.payments:payment.events}")
    private String paymentsExchange;

    public PaymentEventPublisher(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    public void publishPaymentCompleted(PaymentCompletedEvent event) {
        log.info("Publishing payment.completed event for orderId={}", event.getOrderId());
        rabbitTemplate.convertAndSend(paymentsExchange, "payment.completed", event);
    }

    public void publishPaymentFailed(PaymentFailedEvent event) {
        log.info("Publishing payment.failed event for orderId={}", event.getOrderId());
        rabbitTemplate.convertAndSend(paymentsExchange, "payment.failed", event);
    }
}
