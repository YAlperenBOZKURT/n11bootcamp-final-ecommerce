package com.yabozkurt.n11bootcamp.ecommerce.product.application.service;

import com.yabozkurt.n11bootcamp.ecommerce.product.application.service.impl.CategoryServiceImpl;
import com.yabozkurt.n11bootcamp.ecommerce.product.domain.exception.CategoryNotFoundException;
import com.yabozkurt.n11bootcamp.ecommerce.product.domain.model.Category;
import com.yabozkurt.n11bootcamp.ecommerce.product.domain.repository.CategoryRepository;
import com.yabozkurt.n11bootcamp.ecommerce.product.presentation.dto.request.CategoryRequest;
import com.yabozkurt.n11bootcamp.ecommerce.product.presentation.dto.response.CategoryResponse;
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
class CategoryServiceImplTest {

    @Mock CategoryRepository categoryRepository;
    @InjectMocks CategoryServiceImpl categoryService;

    @Test
    void getById_existing_returnsResponse() {
        Category cat = new Category("Elektronik", null);
        cat.setId(1L);
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(cat));

        CategoryResponse res = categoryService.getById(1L);

        assertThat(res.getName()).isEqualTo("Elektronik");
    }

    @Test
    void getById_notFound_throws() {
        when(categoryRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> categoryService.getById(99L))
                .isInstanceOf(CategoryNotFoundException.class);
    }

    @Test
    void create_newName_success() {
        when(categoryRepository.existsByName("Giyim")).thenReturn(false);
        Category saved = new Category("Giyim", null);
        saved.setId(2L);
        when(categoryRepository.save(any())).thenReturn(saved);

        CategoryRequest req = new CategoryRequest();
        req.setName("Giyim");

        CategoryResponse res = categoryService.create(req);

        assertThat(res.getName()).isEqualTo("Giyim");
    }

    @Test
    void create_duplicateName_throws() {
        when(categoryRepository.existsByName("Elektronik")).thenReturn(true);

        CategoryRequest req = new CategoryRequest();
        req.setName("Elektronik");

        assertThatThrownBy(() -> categoryService.create(req))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void getRootCategories_returnsOnlyRoots() {
        Category root = new Category("Root", null);
        root.setId(1L);
        when(categoryRepository.findByParentIsNull()).thenReturn(List.of(root));

        List<CategoryResponse> roots = categoryService.getRootCategories();

        assertThat(roots).hasSize(1);
        assertThat(roots.get(0).getParentId()).isNull();
    }

    @Test
    void delete_existing_callsDelete() {
        Category cat = new Category("Silinecek", null);
        cat.setId(5L);
        when(categoryRepository.findById(5L)).thenReturn(Optional.of(cat));

        categoryService.delete(5L);

        verify(categoryRepository).deleteById(5L);
    }
}
