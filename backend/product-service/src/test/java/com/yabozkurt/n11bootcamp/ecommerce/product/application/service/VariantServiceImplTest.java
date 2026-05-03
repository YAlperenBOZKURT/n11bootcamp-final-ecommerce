package com.yabozkurt.n11bootcamp.ecommerce.product.application.service;

import com.yabozkurt.n11bootcamp.ecommerce.product.application.service.impl.VariantServiceImpl;
import com.yabozkurt.n11bootcamp.ecommerce.product.domain.exception.ProductNotFoundException;
import com.yabozkurt.n11bootcamp.ecommerce.product.domain.exception.VariantNotFoundException;
import com.yabozkurt.n11bootcamp.ecommerce.product.domain.model.Product;
import com.yabozkurt.n11bootcamp.ecommerce.product.domain.model.ProductVariant;
import com.yabozkurt.n11bootcamp.ecommerce.product.domain.repository.ProductRepository;
import com.yabozkurt.n11bootcamp.ecommerce.product.domain.repository.ProductVariantRepository;
import com.yabozkurt.n11bootcamp.ecommerce.product.infrastructure.messaging.publisher.ProductEventPublisher;
import com.yabozkurt.n11bootcamp.ecommerce.product.presentation.dto.request.VariantRequest;
import com.yabozkurt.n11bootcamp.ecommerce.product.presentation.dto.response.VariantResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class VariantServiceImplTest {

    @Mock ProductVariantRepository variantRepository;
    @Mock ProductRepository productRepository;
    @Mock ProductEventPublisher eventPublisher;

    @InjectMocks VariantServiceImpl variantService;

    private Product product;
    private ProductVariant variant;

    @BeforeEach
    void setUp() {
        product = new Product();
        product.setId(1L);
        product.setName("Tişört");
        product.setBrand("Brand");

        variant = new ProductVariant();
        variant.setId(10L);
        variant.setProduct(product);
        variant.setSku("TSH-M-RED");
        variant.setPrice(new BigDecimal("299"));
        variant.setAttributes(Map.of("beden", "M", "renk", "kırmızı"));
    }

    @Test
    void getByProductId_returnsVariants() {
        when(productRepository.findById(1L)).thenReturn(Optional.of(product));
        when(variantRepository.findByProductId(1L)).thenReturn(List.of(variant));

        List<VariantResponse> result = variantService.getByProductId(1L);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getSku()).isEqualTo("TSH-M-RED");
    }

    @Test
    void getByProductId_productNotFound_throws() {
        when(productRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> variantService.getByProductId(99L))
                .isInstanceOf(ProductNotFoundException.class);
    }

    @Test
    void create_validRequest_savesVariant() {
        when(productRepository.findById(1L)).thenReturn(Optional.of(product));
        when(variantRepository.existsBySku("TSH-L-RED")).thenReturn(false);
        when(variantRepository.save(any())).thenAnswer(inv -> {
            ProductVariant v = inv.getArgument(0);
            v.setId(20L);
            return v;
        });

        VariantRequest req = new VariantRequest();
        req.setSku("TSH-L-RED");
        req.setPrice(new BigDecimal("349"));
        req.setAttributes(Map.of("beden", "L"));

        VariantResponse res = variantService.create(1L, req);

        assertThat(res.getSku()).isEqualTo("TSH-L-RED");
        assertThat(res.getEffectivePrice()).isEqualByComparingTo("349");
    }

    @Test
    void create_withDiscount_appliesDiscountedPrice() {
        when(productRepository.findById(1L)).thenReturn(Optional.of(product));
        when(variantRepository.existsBySku("TSH-L-RED")).thenReturn(false);
        when(variantRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        VariantRequest req = new VariantRequest();
        req.setSku("TSH-L-RED");
        req.setPrice(new BigDecimal("400"));
        req.setDiscountRate(new BigDecimal("20"));
        req.setAttributes(Map.of("beden", "L"));

        VariantResponse res = variantService.create(1L, req);

        assertThat(res.getEffectivePrice()).isEqualByComparingTo("320.00");
        assertThat(res.getOriginalPrice()).isEqualByComparingTo("400");
    }

    @Test
    void create_duplicateSku_throws() {
        when(productRepository.findById(1L)).thenReturn(Optional.of(product));
        when(variantRepository.existsBySku("TSH-M-RED")).thenReturn(true);

        VariantRequest req = new VariantRequest();
        req.setSku("TSH-M-RED");
        req.setPrice(new BigDecimal("299"));

        assertThatThrownBy(() -> variantService.create(1L, req))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("TSH-M-RED");
    }

    @Test
    void delete_existing_deletesVariant() {
        when(variantRepository.findByIdAndProductId(10L, 1L)).thenReturn(Optional.of(variant));

        variantService.delete(1L, 10L);

        verify(variantRepository).delete(variant);
    }

    @Test
    void getById_notFound_throws() {
        when(variantRepository.findByIdAndProductId(99L, 1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> variantService.getById(1L, 99L))
                .isInstanceOf(VariantNotFoundException.class);
    }
}
