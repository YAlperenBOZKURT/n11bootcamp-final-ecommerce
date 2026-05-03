package com.yabozkurt.n11bootcamp.stock.infrastructure.messaging.listener;

import com.yabozkurt.n11bootcamp.stock.application.service.StockService;
import com.yabozkurt.n11bootcamp.stock.infrastructure.messaging.event.VariantCreatedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
public class StockEventListener {

    private static final Logger log = LoggerFactory.getLogger(StockEventListener.class);

    private final StockService stockService;

    public StockEventListener(StockService stockService) {
        this.stockService = stockService;
    }

    @RabbitListener(queues = "${rabbitmq.queues.variant-created:variant.created}")
    public void onVariantCreated(VariantCreatedEvent event) {
        log.info("variant.created event: productId={} variantId={}", event.getProductId(), event.getVariantId());
        stockService.initVariantStock(event.getProductId(), event.getVariantId());
    }
}
