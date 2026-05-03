package com.yabozkurt.n11bootcamp.ecommerce.product.application.service;

import com.yabozkurt.n11bootcamp.ecommerce.product.application.service.impl.ProductServiceImpl;
import com.yabozkurt.n11bootcamp.ecommerce.product.domain.exception.ProductNotFoundException;
import com.yabozkurt.n11bootcamp.ecommerce.product.domain.model.Category;
import com.yabozkurt.n11bootcamp.ecommerce.product.domain.model.Product;
import com.yabozkurt.n11bootcamp.ecommerce.product.domain.model.enums.ProductStatus;
import com.yabozkurt.n11bootcamp.ecommerce.product.domain.repository.CategoryRepository;
import com.yabozkurt.n11bootcamp.ecommerce.product.domain.repository.ProductRepository;
import com.yabozkurt.n11bootcamp.ecommerce.product.domain.repository.ProductVariantRepository;
import com.yabozkurt.n11bootcamp.ecommerce.product.infrastructure.elasticsearch.service.ProductIndexService;
import com.yabozkurt.n11bootcamp.ecommerce.product.infrastructure.messaging.publisher.ProductEventPublisher;
import com.yabozkurt.n11bootcamp.ecommerce.product.infrastructure.minio.MinioService;
import com.yabozkurt.n11bootcamp.ecommerce.product.presentation.dto.request.ProductRequest;
import com.yabozkurt.n11bootcamp.ecommerce.product.presentation.dto.response.ProductResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProductServiceImplTest {

    @Mock ProductRepository productRepository;
    @Mock CategoryRepository categoryRepository;
    @Mock ProductVariantRepository variantRepository;
    @Mock MinioService minioService;
    @Mock ProductEventPublisher eventPublisher;
    @Mock ProductIndexService productIndexService;

    @InjectMocks ProductServiceImpl productService;

    private Product activeProduct;
    private Category category;

    @BeforeEach
    void setUp() {
        category = new Category("Elektronik", null);
        category.setId(1L);

        activeProduct = new Product();
        activeProduct.setId(1L);
        activeProduct.setName("Laptop");
        activeProduct.setDescription("Gaming laptop");
        activeProduct.setBrand("Asus");
        activeProduct.setCategory(category);
        activeProduct.setStatus(ProductStatus.ACTIVE);
    }

    @Test
    void getById_active_returnsResponse() {
        when(productRepository.findByIdAndStatusNot(1L, ProductStatus.DELETED))
                .thenReturn(Optional.of(activeProduct));

        ProductResponse res = productService.getById(1L);

        assertThat(res.getName()).isEqualTo("Laptop");
    }

    @Test
    void getById_notFound_throws() {
        when(productRepository.findByIdAndStatusNot(99L, ProductStatus.DELETED))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> productService.getById(99L))
                .isInstanceOf(ProductNotFoundException.class);
    }

    @Test
    void create_validRequest_savesProduct() {
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(category));
        when(productRepository.save(any())).thenAnswer(inv -> {
            Product p = inv.getArgument(0);
            p.setId(2L);
            return p;
        });

        ProductRequest req = new ProductRequest();
        req.setName("Telefon");
        req.setDescription("Akıllı telefon");
        req.setBrand("Samsung");
        req.setCategoryId(1L);

        ProductResponse res = productService.create(req);

        assertThat(res.getName()).isEqualTo("Telefon");
    }

    @Test
    void delete_active_setsStatusDeleted() {
        when(productRepository.findByIdAndStatusNot(1L, ProductStatus.DELETED))
                .thenReturn(Optional.of(activeProduct));
        when(productRepository.save(any())).thenReturn(activeProduct);

        productService.delete(1L);

        assertThat(activeProduct.getStatus()).isEqualTo(ProductStatus.DELETED);
    }

    @Test
    void update_validRequest_updatesFields() {
        when(productRepository.findByIdAndStatusNot(1L, ProductStatus.DELETED))
                .thenReturn(Optional.of(activeProduct));
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(category));
        when(productRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        ProductRequest req = new ProductRequest();
        req.setName("Laptop Pro");
        req.setDescription("Yüksek performanslı laptop");
        req.setBrand("Asus");
        req.setCategoryId(1L);

        ProductResponse res = productService.update(1L, req);

        assertThat(res.getName()).isEqualTo("Laptop Pro");
    }
}
