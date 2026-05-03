package com.yabozkurt.n11bootcamp.ecommerce.product.infrastructure.messaging.listener;

import com.yabozkurt.n11bootcamp.ecommerce.product.domain.model.ProductVariant;
import com.yabozkurt.n11bootcamp.ecommerce.product.domain.model.enums.ProductStatus;
import com.yabozkurt.n11bootcamp.ecommerce.product.domain.model.enums.VariantStatus;
import com.yabozkurt.n11bootcamp.ecommerce.product.domain.repository.ProductRepository;
import com.yabozkurt.n11bootcamp.ecommerce.product.domain.repository.ProductVariantRepository;
import com.yabozkurt.n11bootcamp.ecommerce.product.infrastructure.elasticsearch.service.ProductIndexService;
import com.yabozkurt.n11bootcamp.ecommerce.product.infrastructure.messaging.event.StockStatusEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Component
public class StockEventListener {

    private static final Logger log = LoggerFactory.getLogger(StockEventListener.class);

    private final ProductRepository productRepository;
    private final ProductVariantRepository variantRepository;
    private final ProductIndexService productIndexService;

    public StockEventListener(ProductRepository productRepository,
                               ProductVariantRepository variantRepository,
                               ProductIndexService productIndexService) {
        this.productRepository = productRepository;
        this.variantRepository = variantRepository;
        this.productIndexService = productIndexService;
    }

    @RabbitListener(queues = "${rabbitmq.queues.stock-status:stock.status}")
    @Transactional
    public void onStockStatusChanged(StockStatusEvent event) {
        log.info("Stock event: productId={} variantId={} type={} available={}",
                event.getProductId(), event.getVariantId(), event.getType(), event.getAvailableQuantity());

        if (event.getVariantId() == null) {
            log.warn("Ignoring stock.status without variantId for productId={}", event.getProductId());
            return;
        }
        handleVariantStockEvent(event);
    }



    // Stock event for a specific variant: update variant status, then check if the parent product's status needs to change based on all variants
    private void handleVariantStockEvent(StockStatusEvent event) {
        variantRepository.findById(event.getVariantId()).ifPresentOrElse(variant -> {
            VariantStatus newVariantStatus = event.getType() == StockStatusEvent.Type.DEPLETED
                    ? VariantStatus.PASSIVE : VariantStatus.ACTIVE;

            if (variant.getStatus() != newVariantStatus) {
                variant.setStatus(newVariantStatus);
                variantRepository.save(variant);
                log.info("Variant {} status → {}", variant.getId(), newVariantStatus);
            }

            // Parent product: are all variants passive? If yes, set product to PASSIVE, otherwise ACTIVE
            productRepository.findById(event.getProductId()).ifPresent(product -> {
                List<ProductVariant> allVariants = variantRepository.findByProductId(event.getProductId());
                boolean anyActive = allVariants.stream()
                        .anyMatch(v -> v.getStatus() == VariantStatus.ACTIVE);
                ProductStatus productStatus = anyActive ? ProductStatus.ACTIVE : ProductStatus.PASSIVE;

                if (product.getStatus() != productStatus) {
                    product.setStatus(productStatus);
                    productRepository.save(product);
                    productIndexService.updateStatus(product.getId(), productStatus.name());
                    log.info("Product {} status → {} (variant aggregate)", product.getId(), productStatus);
                }
            });
        }, () -> log.warn("Variant not found: variantId={}", event.getVariantId()));
    }

}
