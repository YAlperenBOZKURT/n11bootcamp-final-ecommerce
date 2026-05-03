package com.yabozkurt.n11bootcamp.ecommerce.payment.infrastructure.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
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

    @Value("${rabbitmq.exchange.payments:payment.events}")
    private String paymentsExchange;

    @Value("${rabbitmq.queues.payment-completed:payment.completed}")
    private String paymentCompletedQueue;

    @Value("${rabbitmq.queues.payment-failed:payment.failed}")
    private String paymentFailedQueue;

    @Bean
    public TopicExchange paymentsExchange() {
        return new TopicExchange(paymentsExchange, true, false);
    }

    @Bean
    public Queue paymentCompletedQueue() {
        return QueueBuilder.durable(paymentCompletedQueue).build();
    }

    @Bean
    public Queue paymentFailedQueue() {
        return QueueBuilder.durable(paymentFailedQueue).build();
    }

    @Bean
    public Binding paymentCompletedBinding() {
        return BindingBuilder.bind(paymentCompletedQueue()).to(paymentsExchange()).with("payment.completed");
    }

    @Bean
    public Binding paymentFailedBinding() {
        return BindingBuilder.bind(paymentFailedQueue()).to(paymentsExchange()).with("payment.failed");
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
