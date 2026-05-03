package com.yabozkurt.n11bootcamp.stock.domain.repository;

import com.yabozkurt.n11bootcamp.stock.domain.model.Stock;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface StockRepository extends JpaRepository<Stock, Long> {

    Optional<Stock> findByVariantId(Long variantId);
    boolean existsByVariantId(Long variantId);

    List<Stock> findAllByProductIdAndVariantIdIsNotNull(Long productId);
}
