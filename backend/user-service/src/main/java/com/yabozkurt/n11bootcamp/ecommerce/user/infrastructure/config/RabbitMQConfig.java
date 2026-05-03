package com.yabozkurt.n11bootcamp.ecommerce.user.infrastructure.config;

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

    @Value("${rabbitmq.exchange.users:user.events}")
    private String usersExchange;

    @Value("${rabbitmq.queues.user-registered:user.registered}")
    private String userRegisteredQueue;

    @Value("${rabbitmq.queues.password-reset-requested:user.password-reset-requested}")
    private String passwordResetRequestedQueue;

    @Bean
    public TopicExchange usersExchange() {
        return new TopicExchange(usersExchange, true, false);
    }

    @Bean
    public Queue userRegisteredQueue() {
        return QueueBuilder.durable(userRegisteredQueue).build();
    }

    @Bean
    public Binding userRegisteredBinding() {
        return BindingBuilder.bind(userRegisteredQueue()).to(usersExchange()).with("user.registered");
    }

    @Bean
    public Queue passwordResetRequestedQueue() {
        return QueueBuilder.durable(passwordResetRequestedQueue).build();
    }

    @Bean
    public Binding passwordResetRequestedBinding() {
        return BindingBuilder.bind(passwordResetRequestedQueue()).to(usersExchange()).with("user.password-reset-requested");
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
