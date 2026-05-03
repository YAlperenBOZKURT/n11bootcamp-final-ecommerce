package com.yabozkurt.n11bootcamp.ecommerce.notification.infrastructure.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.ExchangeBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    private static final String DLX = "notification.dlx";

    @Value("${rabbitmq.exchange.orders:order.events}")
    private String ordersExchange;

    @Value("${rabbitmq.exchange.users:user.events}")
    private String usersExchange;

    @Value("${rabbitmq.queues.order-confirmed:notification.order.confirmed}")
    private String orderConfirmedQueue;

    @Value("${rabbitmq.queues.order-failed:notification.order.failed}")
    private String orderFailedQueue;

    @Value("${rabbitmq.queues.order-cancelled:notification.order.cancelled}")
    private String orderCancelledQueue;

    @Value("${rabbitmq.queues.user-registered:notification.user.registered}")
    private String userRegisteredQueue;

    @Value("${rabbitmq.queues.password-reset-requested:notification.user.password-reset-requested}")
    private String passwordResetRequestedQueue;

    // -- Dead Letter Exchange ---------------------------------------------------

    @Bean
    public DirectExchange notificationDlx() {
        return new DirectExchange(DLX, true, false);
    }

    @Bean
    public Queue orderConfirmedDlq() {
        return QueueBuilder.durable(orderConfirmedQueue + ".dlq").build();
    }

    
    @Bean
    public Queue orderFailedDlq() {
        return QueueBuilder.durable(orderFailedQueue + ".dlq").build();
    }

    @Bean
    public Queue orderCancelledDlq() {
        return QueueBuilder.durable(orderCancelledQueue + ".dlq").build();
    }

    @Bean
    public Queue userRegisteredDlq() {
        return QueueBuilder.durable(userRegisteredQueue + ".dlq").build();
    }

    @Bean
    public Queue passwordResetRequestedDlq() {
        return QueueBuilder.durable(passwordResetRequestedQueue + ".dlq").build();
    }

    @Bean
    public Binding orderConfirmedDlqBinding() {
        return BindingBuilder.bind(orderConfirmedDlq()).to(notificationDlx()).with(orderConfirmedQueue);
    }

    @Bean
    public Binding orderFailedDlqBinding() {
        return BindingBuilder.bind(orderFailedDlq()).to(notificationDlx()).with(orderFailedQueue);
    }

    @Bean
    public Binding orderCancelledDlqBinding() {
        return BindingBuilder.bind(orderCancelledDlq()).to(notificationDlx()).with(orderCancelledQueue);
    }

    @Bean
    public Binding userRegisteredDlqBinding() {
        return BindingBuilder.bind(userRegisteredDlq()).to(notificationDlx()).with(userRegisteredQueue);
    }

    @Bean
    public Binding passwordResetRequestedDlqBinding() {
        return BindingBuilder.bind(passwordResetRequestedDlq()).to(notificationDlx()).with(passwordResetRequestedQueue);
    }

    // -- Main Queues (with DLX routing) ----------------------------------------

    @Bean
    public TopicExchange ordersExchange() {
        return ExchangeBuilder.topicExchange(ordersExchange).durable(true).build();
    }

    @Bean
    public TopicExchange usersExchange() {
        return ExchangeBuilder.topicExchange(usersExchange).durable(true).build();
    }

    @Bean
    public Queue notificationOrderConfirmedQueue() {
        return QueueBuilder.durable(orderConfirmedQueue)
                .withArgument("x-dead-letter-exchange", DLX)
                .withArgument("x-dead-letter-routing-key", orderConfirmedQueue)
                .build();
    }

    @Bean
    public Queue notificationOrderFailedQueue() {
        return QueueBuilder.durable(orderFailedQueue)
                .withArgument("x-dead-letter-exchange", DLX)
                .withArgument("x-dead-letter-routing-key", orderFailedQueue)
                .build();
    }

    @Bean
    public Queue notificationOrderCancelledQueue() {
        return QueueBuilder.durable(orderCancelledQueue)
                .withArgument("x-dead-letter-exchange", DLX)
                .withArgument("x-dead-letter-routing-key", orderCancelledQueue)
                .build();
    }

    @Bean
    public Queue notificationUserRegisteredQueue() {
        return QueueBuilder.durable(userRegisteredQueue)
                .withArgument("x-dead-letter-exchange", DLX)
                .withArgument("x-dead-letter-routing-key", userRegisteredQueue)
                .build();
    }

    @Bean
    public Queue notificationPasswordResetRequestedQueue() {
        return QueueBuilder.durable(passwordResetRequestedQueue)
                .withArgument("x-dead-letter-exchange", DLX)
                .withArgument("x-dead-letter-routing-key", passwordResetRequestedQueue)
                .build();
    }

    @Bean
    public Binding notificationOrderConfirmedBinding() {
        return BindingBuilder.bind(notificationOrderConfirmedQueue()).to(ordersExchange()).with("order.confirmed");
    }

    @Bean
    public Binding notificationOrderFailedBinding() {
        return BindingBuilder.bind(notificationOrderFailedQueue()).to(ordersExchange()).with("order.failed");
    }

    @Bean
    public Binding notificationOrderCancelledBinding() {
        return BindingBuilder.bind(notificationOrderCancelledQueue()).to(ordersExchange()).with("order.cancelled");
    }

    @Bean
    public Binding notificationUserRegisteredBinding() {
        return BindingBuilder.bind(notificationUserRegisteredQueue()).to(usersExchange()).with("user.registered");
    }

    @Bean
    public Binding notificationPasswordResetRequestedBinding() {
        return BindingBuilder.bind(notificationPasswordResetRequestedQueue()).to(usersExchange()).with("user.password-reset-requested");
    }

    @Bean
    public Jackson2JsonMessageConverter messageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    @Bean
    @ConditionalOnBean(ConnectionFactory.class)
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(messageConverter());
        return template;
    }
}
