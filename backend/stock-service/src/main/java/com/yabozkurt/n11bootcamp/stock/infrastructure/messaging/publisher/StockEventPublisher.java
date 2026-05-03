package com.yabozkurt.n11bootcamp.stock.infrastructure.messaging.publisher;

import com.yabozkurt.n11bootcamp.stock.infrastructure.messaging.event.StockStatusEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class StockEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(StockEventPublisher.class);

    private final RabbitTemplate rabbitTemplate;

    @Value("${rabbitmq.exchange.stock:stock.events}")
    private String stockExchange;

    public StockEventPublisher(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    public void publishStockStatus(StockStatusEvent event) {
        log.info("Publishing stock.status: productId={} variantId={} type={} available={}",
                event.getProductId(), event.getVariantId(), event.getType(), event.getAvailableQuantity());
        rabbitTemplate.convertAndSend(stockExchange, "stock.status", event);
    }
}
