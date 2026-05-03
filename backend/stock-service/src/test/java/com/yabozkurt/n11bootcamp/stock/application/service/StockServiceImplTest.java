package com.yabozkurt.n11bootcamp.stock.application.service;

import com.yabozkurt.n11bootcamp.stock.application.service.impl.StockServiceImpl;
import com.yabozkurt.n11bootcamp.stock.domain.exception.InsufficientStockException;
import com.yabozkurt.n11bootcamp.stock.domain.exception.StockNotFoundException;
import com.yabozkurt.n11bootcamp.stock.domain.model.Stock;
import com.yabozkurt.n11bootcamp.stock.domain.model.StockMovement;
import com.yabozkurt.n11bootcamp.stock.domain.model.enums.MovementType;
import com.yabozkurt.n11bootcamp.stock.domain.repository.StockMovementRepository;
import com.yabozkurt.n11bootcamp.stock.domain.repository.StockRepository;
import com.yabozkurt.n11bootcamp.stock.infrastructure.messaging.publisher.StockEventPublisher;
import com.yabozkurt.n11bootcamp.stock.presentation.dto.request.ReserveRequest;
import com.yabozkurt.n11bootcamp.stock.presentation.dto.request.StockUpdateRequest;
import com.yabozkurt.n11bootcamp.stock.presentation.dto.response.StockResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class StockServiceImplTest {

    @Mock StockRepository stockRepository;
    @Mock StockMovementRepository movementRepository;
    @Mock StockEventPublisher eventPublisher;

    @InjectMocks StockServiceImpl stockService;

    private Stock stock;

    @BeforeEach
    void setUp() {
        stock = new Stock();
        stock.setId(1L);
        stock.setProductId(10L);
        stock.setVariantId(100L);
        stock.setQuantity(50);
        stock.setReservedQuantity(10);
    }

    @Test
    void getByVariantId_found_returnsResponse() {
        when(stockRepository.findByVariantId(100L)).thenReturn(Optional.of(stock));

        StockResponse res = stockService.getByVariantId(100L);

        assertThat(res.getQuantity()).isEqualTo(50);
        assertThat(res.getAvailableQuantity()).isEqualTo(40);
    }

    @Test
    void getByVariantId_notFound_throws() {
        when(stockRepository.findByVariantId(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> stockService.getByVariantId(999L))
                .isInstanceOf(StockNotFoundException.class);
    }

    @Test
    void addStockByVariant_increasesQuantity() {
        when(stockRepository.findByVariantId(100L)).thenReturn(Optional.of(stock));
        when(stockRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        StockUpdateRequest req = new StockUpdateRequest();
        req.setQuantity(20);

        StockResponse res = stockService.addStockByVariant(100L, req);

        assertThat(res.getQuantity()).isEqualTo(70);
        verify(movementRepository).save(argThat(m -> m.getType() == MovementType.IN && m.getQuantity() == 20));
    }

    @Test
    void reserveByVariant_sufficient_reservesQuantity() {
        when(stockRepository.findByVariantId(100L)).thenReturn(Optional.of(stock));
        when(stockRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        ReserveRequest req = new ReserveRequest();
        req.setQuantity(5);

        StockResponse res = stockService.reserveByVariant(100L, req);

        assertThat(res.getReservedQuantity()).isEqualTo(15);
        verify(movementRepository).save(argThat(m -> m.getType() == MovementType.RESERVE));
    }

    @Test
    void reserveByVariant_insufficient_throws() {
        when(stockRepository.findByVariantId(100L)).thenReturn(Optional.of(stock));

        ReserveRequest req = new ReserveRequest();
        req.setQuantity(100);

        assertThatThrownBy(() -> stockService.reserveByVariant(100L, req))
                .isInstanceOf(InsufficientStockException.class);
    }

    @Test
    void releaseByVariant_decreasesReserved() {
        when(stockRepository.findByVariantId(100L)).thenReturn(Optional.of(stock));
        when(stockRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        ReserveRequest req = new ReserveRequest();
        req.setQuantity(5);

        StockResponse res = stockService.releaseByVariant(100L, req);

        assertThat(res.getReservedQuantity()).isEqualTo(5);
        verify(movementRepository).save(argThat(m -> m.getType() == MovementType.RELEASE));
    }

    @Test
    void confirmByVariant_decreasesQuantityAndReserved() {
        when(stockRepository.findByVariantId(100L)).thenReturn(Optional.of(stock));
        when(stockRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        ReserveRequest req = new ReserveRequest();
        req.setQuantity(5);

        StockResponse res = stockService.confirmByVariant(100L, req);

        assertThat(res.getQuantity()).isEqualTo(45);
        assertThat(res.getReservedQuantity()).isEqualTo(5);
        verify(movementRepository).save(argThat(m -> m.getType() == MovementType.CONFIRM));
    }

    @Test
    void initVariantStock_variantNotExists_createsStock() {
        when(stockRepository.existsByVariantId(200L)).thenReturn(false);

        stockService.initVariantStock(20L, 200L);

        verify(stockRepository).save(argThat(s -> s.getProductId().equals(20L) && s.getVariantId().equals(200L)));
    }

    @Test
    void initVariantStock_alreadyExists_doesNothing() {
        when(stockRepository.existsByVariantId(100L)).thenReturn(true);

        stockService.initVariantStock(10L, 100L);

        verify(stockRepository, never()).save(any());
    }
}
