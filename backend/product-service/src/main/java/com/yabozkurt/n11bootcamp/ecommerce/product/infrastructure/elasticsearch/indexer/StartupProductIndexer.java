package com.yabozkurt.n11bootcamp.ecommerce.product.infrastructure.elasticsearch.indexer;

import com.yabozkurt.n11bootcamp.ecommerce.product.domain.model.Product;
import com.yabozkurt.n11bootcamp.ecommerce.product.domain.repository.ProductRepository;
import com.yabozkurt.n11bootcamp.ecommerce.product.infrastructure.elasticsearch.service.ProductIndexService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Indexes all products into Elasticsearch on startup.
 * Retries up to 5 times with a 3-second delay so ES has time to warm up.
 */
@Component
public class StartupProductIndexer implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(StartupProductIndexer.class);
    private static final int MAX_RETRIES = 5; // Max attempts to index before giving up
    private static final long RETRY_DELAY_MS = 3_000; // 3 seconds delay between attempts

    private final ProductRepository productRepository;
    private final ProductIndexService productIndexService;

    public StartupProductIndexer(ProductRepository productRepository,
                                 ProductIndexService productIndexService) {
        this.productRepository = productRepository;
        this.productIndexService = productIndexService;
    }

    @Override
    public void run(ApplicationArguments args) {
        // All products with their categories and variants for ELASTICSEARCH indexing
        List<Product> allProducts = productRepository.findAllWithCategoryAndVariants();

        if (allProducts.isEmpty()) {
            log.info("ES startup indexer: no products found, skipping.");
            return;
        }

        // Retry indexing in case ELASTICSEARCH is not ready yet
        for (int attempt = 1; attempt <= MAX_RETRIES; attempt++) {
            try {
                productIndexService.bulkIndex(allProducts);
                log.info("ES startup indexer: indexed {} products (attempt {}).", allProducts.size(), attempt);
                return;
            } catch (Exception ex) {
                log.warn("ES startup indexer attempt {}/{} failed: {}", attempt, MAX_RETRIES, ex.getMessage());
                if (attempt < MAX_RETRIES) {
                    try { Thread.sleep(RETRY_DELAY_MS); } catch (InterruptedException ie) { Thread.currentThread().interrupt(); return; }
                }
            }
        }
        log.warn("ES startup indexer gave up after {} attempts. Keyword search will fall back to DB.", MAX_RETRIES);
    }
}
