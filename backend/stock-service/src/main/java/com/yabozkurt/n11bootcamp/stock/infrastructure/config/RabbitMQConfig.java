package com.yabozkurt.n11bootcamp.stock.infrastructure.config;

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

    // -- Exchanges -------------------------------------------------------------

    @Value("${rabbitmq.exchange.stock:stock.events}")
    private String stockExchange;

    @Value("${rabbitmq.exchange.products:product.events}")
    private String productsExchange;

    // -- Queue names ----------------------------------------------------------

    @Value("${rabbitmq.queues.stock-status:stock.status}")
    private String stockStatusQueue;

    @Value("${rabbitmq.queues.product-created:product.created}")
    private String productCreatedQueue;

    @Value("${rabbitmq.queues.variant-created:variant.created}")
    private String variantCreatedQueue;

    // -- stock.events exchange (producer) -------------------------------------

    @Bean
    public TopicExchange stockExchange() {
        return new TopicExchange(stockExchange, true, false);
    }

    @Bean
    public Queue stockStatusQueue() {
        return QueueBuilder.durable(stockStatusQueue).build();
    }

    @Bean
    public Binding stockStatusBinding() {
        return BindingBuilder.bind(stockStatusQueue()).to(stockExchange()).with("stock.status");
    }

    // -- product.events exchange (consumer) -----------------------------------

    @Bean
    public TopicExchange productsExchange() {
        return new TopicExchange(productsExchange, true, false);
    }

    @Bean
    public Queue productCreatedQueue() {
        return QueueBuilder.durable(productCreatedQueue).build();
    }

    @Bean
    public Binding productCreatedBinding() {
        return BindingBuilder.bind(productCreatedQueue()).to(productsExchange()).with("product.created");
    }

    @Bean
    public Queue variantCreatedQueue() {
        return QueueBuilder.durable(variantCreatedQueue).build();
    }

    @Bean
    public Binding variantCreatedBinding() {
        return BindingBuilder.bind(variantCreatedQueue()).to(productsExchange()).with("variant.created");
    }

    // -- Shared ----------------------------------------------------------------

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
