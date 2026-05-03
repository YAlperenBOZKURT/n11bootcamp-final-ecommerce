package com.yabozkurt.n11bootcamp.ecommerce.product.application.service.impl;

import com.yabozkurt.n11bootcamp.ecommerce.product.application.service.VariantService;
import com.yabozkurt.n11bootcamp.ecommerce.product.domain.exception.ProductNotFoundException;
import com.yabozkurt.n11bootcamp.ecommerce.product.domain.exception.VariantNotFoundException;
import com.yabozkurt.n11bootcamp.ecommerce.product.domain.model.Product;
import com.yabozkurt.n11bootcamp.ecommerce.product.domain.model.ProductVariant;
import com.yabozkurt.n11bootcamp.ecommerce.product.domain.model.enums.VariantStatus;
import com.yabozkurt.n11bootcamp.ecommerce.product.domain.repository.ProductRepository;
import com.yabozkurt.n11bootcamp.ecommerce.product.domain.repository.ProductVariantRepository;
import com.yabozkurt.n11bootcamp.ecommerce.product.infrastructure.messaging.event.VariantCreatedEvent;
import com.yabozkurt.n11bootcamp.ecommerce.product.infrastructure.messaging.publisher.ProductEventPublisher;
import com.yabozkurt.n11bootcamp.ecommerce.product.presentation.dto.request.VariantRequest;
import com.yabozkurt.n11bootcamp.ecommerce.product.presentation.dto.response.VariantResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

@Service
@Transactional(readOnly = true)
public class VariantServiceImpl implements VariantService {

    private final ProductVariantRepository variantRepository;
    private final ProductRepository productRepository;
    private final ProductEventPublisher eventPublisher;

    public VariantServiceImpl(ProductVariantRepository variantRepository,
                              ProductRepository productRepository,
                              ProductEventPublisher eventPublisher) {
        this.variantRepository = variantRepository;
        this.productRepository = productRepository;
        this.eventPublisher = eventPublisher;
    }

    @Override
    public List<VariantResponse> getByProductId(Long productId) {
        findProduct(productId);
        return variantRepository.findByProductId(productId)
                .stream().map(VariantServiceImpl::toResponse).toList();
    }

    @Override
    public VariantResponse getById(Long productId, Long variantId) {
        return toResponse(findVariant(productId, variantId));
    }

    @Override
    @Transactional
    public VariantResponse create(Long productId, VariantRequest request) {
        Product product = findProduct(productId);
        if (variantRepository.existsBySku(request.getSku())) {
            throw new IllegalStateException("SKU already exists: " + request.getSku());
        }
        if (request.getAttributes() == null || request.getAttributes().isEmpty()) {
            throw new IllegalArgumentException("Varyant için en az bir özellik (attribute) girilmelidir.");
        }

        ProductVariant variant = new ProductVariant();
        variant.setProduct(product);
        variant.setSku(request.getSku());
        variant.setAttributes(request.getAttributes());
        applyPriceAndDiscount(variant, request);
        variant.setStatus(VariantStatus.PASSIVE);

        ProductVariant saved = variantRepository.save(variant);
        eventPublisher.publishVariantCreated(new VariantCreatedEvent(productId, saved.getId()));
        return toResponse(saved);
    }

    @Override
    @Transactional
    public VariantResponse update(Long productId, Long variantId, VariantRequest request) {
        ProductVariant variant = findVariant(productId, variantId);
        if (!variant.getSku().equals(request.getSku()) && variantRepository.existsBySku(request.getSku())) {
            throw new IllegalStateException("SKU already exists: " + request.getSku());
        }
        variant.setSku(request.getSku());
        if (request.getAttributes() != null) variant.setAttributes(request.getAttributes());
        applyPriceAndDiscount(variant, request);
        return toResponse(variantRepository.save(variant));
    }

    @Override
    @Transactional
    public void delete(Long productId, Long variantId) {
        variantRepository.delete(findVariant(productId, variantId));
    }

    // -- helpers ---------------------------------------------------------------

    private void applyPriceAndDiscount(ProductVariant variant, VariantRequest request) {
        BigDecimal basePrice = request.getPrice();
        BigDecimal discountRate = request.getDiscountRate();

        if (discountRate != null && discountRate.compareTo(BigDecimal.ZERO) > 0) {
            variant.setOriginalPrice(basePrice);
            variant.setDiscountRate(discountRate);
            BigDecimal effective = basePrice
                    .multiply(BigDecimal.ONE.subtract(discountRate.divide(BigDecimal.valueOf(100))))
                    .setScale(2, RoundingMode.HALF_UP);
            variant.setPrice(effective);
        } else {
            variant.setPrice(basePrice);
            variant.setOriginalPrice(null);
            variant.setDiscountRate(null);
        }

        variant.setDiscountStartAt(request.getDiscountStartAt());
        variant.setDiscountEndAt(request.getDiscountEndAt());
    }

    private Product findProduct(Long productId) {
        return productRepository.findById(productId)
                .orElseThrow(() -> new ProductNotFoundException(productId));
    }

    private ProductVariant findVariant(Long productId, Long variantId) {
        return variantRepository.findByIdAndProductId(variantId, productId)
                .orElseThrow(() -> new VariantNotFoundException(variantId));
    }

    public static VariantResponse toResponse(ProductVariant v) {
        VariantResponse res = new VariantResponse();
        res.setId(v.getId());
        res.setProductId(v.getProduct().getId());
        res.setSku(v.getSku());
        res.setAttributes(v.getAttributes());
        res.setPrice(v.getPrice());
        res.setOriginalPrice(v.getOriginalPrice());
        res.setDiscountRate(v.getDiscountRate());
        res.setDiscountStartAt(v.getDiscountStartAt());
        res.setDiscountEndAt(v.getDiscountEndAt());
        // effectivePrice = price (indirim zaten price'a uygulandı)
        res.setEffectivePrice(v.getPrice());
        res.setStatus(v.getStatus() != null ? v.getStatus().name() : VariantStatus.PASSIVE.name());
        return res;
    }
}
