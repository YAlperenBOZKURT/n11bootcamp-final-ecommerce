package com.yabozkurt.n11bootcamp.stock.domain.repository;

import com.yabozkurt.n11bootcamp.stock.domain.model.StockMovement;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface StockMovementRepository extends JpaRepository<StockMovement, Long> {

    List<StockMovement> findByStockIdOrderByCreatedAtDesc(Long stockId);
}
