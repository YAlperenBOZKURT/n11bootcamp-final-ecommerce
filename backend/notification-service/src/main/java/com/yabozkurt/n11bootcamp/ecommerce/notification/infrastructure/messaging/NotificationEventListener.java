package com.yabozkurt.n11bootcamp.ecommerce.notification.infrastructure.messaging;

import com.yabozkurt.n11bootcamp.ecommerce.notification.application.service.NotificationService;
import com.yabozkurt.n11bootcamp.ecommerce.notification.infrastructure.messaging.event.OrderCancelledEvent;
import com.yabozkurt.n11bootcamp.ecommerce.notification.infrastructure.messaging.event.OrderConfirmedEvent;
import com.yabozkurt.n11bootcamp.ecommerce.notification.infrastructure.messaging.event.OrderFailedEvent;
import com.yabozkurt.n11bootcamp.ecommerce.notification.infrastructure.messaging.event.PasswordResetRequestedEvent;
import com.yabozkurt.n11bootcamp.ecommerce.notification.infrastructure.messaging.event.UserRegisteredEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
public class NotificationEventListener {

    private static final Logger log = LoggerFactory.getLogger(NotificationEventListener.class);

    private final NotificationService notificationService;

    public NotificationEventListener(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @RabbitListener(queues = "${rabbitmq.queues.order-confirmed}")
    public void onOrderConfirmed(OrderConfirmedEvent event) {
        log.info("Received order.confirmed event: orderId={}", event.getOrderId());
        notificationService.sendOrderConfirmed(event);
    }

    @RabbitListener(queues = "${rabbitmq.queues.order-failed}")
    public void onOrderFailed(OrderFailedEvent event) {
        log.info("Received order.failed event: orderId={}", event.getOrderId());
        notificationService.sendOrderFailed(event);
    }

    @RabbitListener(queues = "${rabbitmq.queues.order-cancelled}")
    public void onOrderCancelled(OrderCancelledEvent event) {
        log.info("Received order.cancelled event: orderId={}", event.getOrderId());
        notificationService.sendOrderCancelled(event);
    }

    @RabbitListener(queues = "${rabbitmq.queues.user-registered}")
    public void onUserRegistered(UserRegisteredEvent event) {
        log.info("Received user.registered event: userId={}", event.getUserId());
        notificationService.sendWelcome(event);
    }

    @RabbitListener(queues = "${rabbitmq.queues.password-reset-requested}")
    public void onPasswordResetRequested(PasswordResetRequestedEvent event) {
        log.info("Received user.password-reset-requested event: userId={}", event.getUserId());
        notificationService.sendPasswordReset(event);
    }
}
