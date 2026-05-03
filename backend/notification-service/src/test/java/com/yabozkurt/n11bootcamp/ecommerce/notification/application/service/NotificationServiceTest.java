package com.yabozkurt.n11bootcamp.ecommerce.notification.application.service;

import com.yabozkurt.n11bootcamp.ecommerce.notification.infrastructure.messaging.event.OrderCancelledEvent;
import com.yabozkurt.n11bootcamp.ecommerce.notification.infrastructure.messaging.event.OrderConfirmedEvent;
import com.yabozkurt.n11bootcamp.ecommerce.notification.infrastructure.messaging.event.OrderFailedEvent;
import com.yabozkurt.n11bootcamp.ecommerce.notification.infrastructure.messaging.event.PasswordResetRequestedEvent;
import com.yabozkurt.n11bootcamp.ecommerce.notification.infrastructure.messaging.event.UserRegisteredEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.javamail.JavaMailSender;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

    @Mock
    JavaMailSender mailSender;

    NotificationService notificationService;

    @BeforeEach
    void setUp() {
        notificationService = new NotificationService(mailSender, "noreply@example.com");
    }

    @Test
    void sendOrderConfirmed_doesNotThrow() {
        OrderConfirmedEvent event = new OrderConfirmedEvent();
        event.setOrderId("ORD-001");
        event.setUserId(1L);
        event.setUserEmail("test@example.com");
        event.setTotalAmount(new BigDecimal("299.00"));

        assertDoesNotThrow(() -> notificationService.sendOrderConfirmed(event));
    }

    @Test
    void sendOrderFailed_doesNotThrow() {
        OrderFailedEvent event = new OrderFailedEvent();
        event.setOrderId("ORD-002");
        event.setUserId(1L);
        event.setUserEmail("test@example.com");
        event.setReason("Kart reddedildi");

        assertDoesNotThrow(() -> notificationService.sendOrderFailed(event));
    }

    @Test
    void sendOrderCancelled_doesNotThrow() {
        OrderCancelledEvent event = new OrderCancelledEvent();
        event.setOrderId("ORD-003");
        event.setUserId(1L);
        event.setUserEmail("test@example.com");
        event.setReason("Kullanıcı iptal etti");

        assertDoesNotThrow(() -> notificationService.sendOrderCancelled(event));
    }

    @Test
    void sendWelcome_doesNotThrow() {
        UserRegisteredEvent event = new UserRegisteredEvent();
        event.setUserId(11L);
        event.setEmail("newuser@example.com");
        event.setFirstName("Jane");
        event.setLastName("Doe");

        assertDoesNotThrow(() -> notificationService.sendWelcome(event));
    }

    @Test
    void sendPasswordReset_doesNotThrow() {
        PasswordResetRequestedEvent event = new PasswordResetRequestedEvent();
        event.setUserId(11L);
        event.setEmail("newuser@example.com");
        event.setFirstName("Jane");
        event.setResetToken("token-123");
        event.setExpiresInSeconds(900L);

        assertDoesNotThrow(() -> notificationService.sendPasswordReset(event));
    }
}
