package com.yabozkurt.n11bootcamp.stock.application.service.impl;

import com.yabozkurt.n11bootcamp.stock.application.service.StockService;
import com.yabozkurt.n11bootcamp.stock.domain.exception.InsufficientStockException;
import com.yabozkurt.n11bootcamp.stock.domain.exception.StockNotFoundException;
import com.yabozkurt.n11bootcamp.stock.domain.model.Stock;
import com.yabozkurt.n11bootcamp.stock.domain.model.StockMovement;
import com.yabozkurt.n11bootcamp.stock.domain.model.enums.MovementType;
import com.yabozkurt.n11bootcamp.stock.domain.repository.StockMovementRepository;
import com.yabozkurt.n11bootcamp.stock.domain.repository.StockRepository;
import com.yabozkurt.n11bootcamp.stock.infrastructure.messaging.publisher.StockEventPublisher;
import com.yabozkurt.n11bootcamp.stock.infrastructure.messaging.event.StockStatusEvent;
import com.yabozkurt.n11bootcamp.stock.presentation.dto.request.ReserveRequest;
import com.yabozkurt.n11bootcamp.stock.presentation.dto.request.StockUpdateRequest;
import com.yabozkurt.n11bootcamp.stock.presentation.dto.response.StockMovementResponse;
import com.yabozkurt.n11bootcamp.stock.presentation.dto.response.StockResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class StockServiceImpl implements StockService {

    private final StockRepository stockRepository;
    private final StockMovementRepository movementRepository;
    private final StockEventPublisher eventPublisher;

    public StockServiceImpl(StockRepository stockRepository,
                            StockMovementRepository movementRepository,
                            StockEventPublisher eventPublisher) {
        this.stockRepository = stockRepository;
        this.movementRepository = movementRepository;
        this.eventPublisher = eventPublisher;
    }

    // -- Variant-level operations -----------------------------------------------

    @Override
    @Transactional
    public void initVariantStock(Long productId, Long variantId) {
        if (!stockRepository.existsByVariantId(variantId)) {
            Stock stock = new Stock();
            stock.setProductId(productId);
            stock.setVariantId(variantId);
            stockRepository.save(stock);
        }
    }

    @Override
    public StockResponse getByVariantId(Long variantId) {
        return toResponse(findVariantStock(variantId));
    }

    @Override
    @Transactional
    public StockResponse addStockByVariant(Long variantId, StockUpdateRequest request) {
        Stock stock = findVariantStock(variantId);
        boolean wasEmpty = stock.getAvailableQuantity() <= 0;
        stock.setQuantity(stock.getQuantity() + request.getQuantity());
        stockRepository.save(stock);
        saveMovement(stock, MovementType.IN, request.getQuantity(), request.getNote());
        if (wasEmpty && stock.getAvailableQuantity() > 0) {
            eventPublisher.publishStockStatus(new StockStatusEvent(
                    stock.getProductId(), stock.getVariantId(), StockStatusEvent.Type.REPLENISHED, stock.getAvailableQuantity()));
        }
        return toResponse(stock);
    }

    @Override
    @Transactional
    public StockResponse reserveByVariant(Long variantId, ReserveRequest request) {
        Stock stock = findVariantStock(variantId);
        if (stock.getAvailableQuantity() < request.getQuantity()) {
            throw new InsufficientStockException(stock.getProductId(), request.getQuantity(), stock.getAvailableQuantity());
        }
        stock.setReservedQuantity(stock.getReservedQuantity() + request.getQuantity());
        stockRepository.save(stock);
        saveMovement(stock, MovementType.RESERVE, request.getQuantity(), request.getNote());
        if (stock.getAvailableQuantity() <= 0) {
            eventPublisher.publishStockStatus(new StockStatusEvent(
                    stock.getProductId(), stock.getVariantId(), StockStatusEvent.Type.DEPLETED, 0));
        }
        return toResponse(stock);
    }

    @Override
    @Transactional
    public StockResponse releaseByVariant(Long variantId, ReserveRequest request) {
        Stock stock = findVariantStock(variantId);
        boolean wasEmpty = stock.getAvailableQuantity() <= 0;
        int newReserved = Math.max(0, stock.getReservedQuantity() - request.getQuantity());
        stock.setReservedQuantity(newReserved);
        stockRepository.save(stock);
        saveMovement(stock, MovementType.RELEASE, request.getQuantity(), request.getNote());
        if (wasEmpty && stock.getAvailableQuantity() > 0) {
            eventPublisher.publishStockStatus(new StockStatusEvent(
                    stock.getProductId(), stock.getVariantId(), StockStatusEvent.Type.REPLENISHED, stock.getAvailableQuantity()));
        }
        return toResponse(stock);
    }

    @Override
    @Transactional
    public StockResponse confirmByVariant(Long variantId, ReserveRequest request) {
        Stock stock = findVariantStock(variantId);
        stock.setQuantity(stock.getQuantity() - request.getQuantity());
        stock.setReservedQuantity(stock.getReservedQuantity() - request.getQuantity());
        stockRepository.save(stock);
        saveMovement(stock, MovementType.CONFIRM, request.getQuantity(), request.getNote());
        if (stock.getAvailableQuantity() <= 0) {
            eventPublisher.publishStockStatus(new StockStatusEvent(
                    stock.getProductId(), stock.getVariantId(), StockStatusEvent.Type.DEPLETED, 0));
        }
        return toResponse(stock);
    }

    @Override
    public List<StockMovementResponse> getMovementsByVariantId(Long variantId) {
        Stock stock = findVariantStock(variantId);
        return movementRepository.findByStockIdOrderByCreatedAtDesc(stock.getId())
                .stream().map(StockServiceImpl::toMovementResponse).toList();
    }

    @Override
    public List<StockResponse> getAllVariantStocks(Long productId) {
        return stockRepository.findAllByProductIdAndVariantIdIsNotNull(productId)
                .stream().map(StockServiceImpl::toResponse).toList();
    }

    // -- helpers ---------------------------------------------------------------

    private Stock findVariantStock(Long variantId) {
        return stockRepository.findByVariantId(variantId)
                .orElseThrow(() -> new StockNotFoundException(variantId));
    }

    private void saveMovement(Stock stock, MovementType type, int quantity, String note) {
        StockMovement movement = new StockMovement();
        movement.setStock(stock);
        movement.setType(type);
        movement.setQuantity(quantity);
        movement.setNote(note);
        movementRepository.save(movement);
    }

    public static StockResponse toResponse(Stock s) {
        StockResponse res = new StockResponse();
        res.setId(s.getId());
        res.setProductId(s.getProductId());
        res.setVariantId(s.getVariantId());
        res.setQuantity(s.getQuantity());
        res.setReservedQuantity(s.getReservedQuantity());
        res.setAvailableQuantity(s.getAvailableQuantity());
        res.setUpdatedAt(s.getUpdatedAt());
        return res;
    }

    public static StockMovementResponse toMovementResponse(StockMovement m) {
        StockMovementResponse res = new StockMovementResponse();
        res.setId(m.getId());
        res.setType(m.getType());
        res.setQuantity(m.getQuantity());
        res.setNote(m.getNote());
        res.setCreatedAt(m.getCreatedAt());
        return res;
    }
}
