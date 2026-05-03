package com.yabozkurt.n11bootcamp.ecommerce.user.infrastructure.messaging.publisher;

import com.yabozkurt.n11bootcamp.ecommerce.user.infrastructure.messaging.event.UserRegisteredEvent;
import com.yabozkurt.n11bootcamp.ecommerce.user.infrastructure.messaging.event.PasswordResetRequestedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class UserEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(UserEventPublisher.class);

    private final RabbitTemplate rabbitTemplate;

    @Value("${rabbitmq.exchange.users:user.events}")
    private String usersExchange;

    public UserEventPublisher(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    public void publishUserRegistered(UserRegisteredEvent event) {
        log.info("Publishing user.registered event for userId={}", event.getUserId());
        rabbitTemplate.convertAndSend(usersExchange, "user.registered", event);
    }

    public void publishPasswordResetRequested(PasswordResetRequestedEvent event) {
        log.info("Publishing user.password-reset-requested event for userId={}", event.getUserId());
        rabbitTemplate.convertAndSend(usersExchange, "user.password-reset-requested", event);
    }
}
