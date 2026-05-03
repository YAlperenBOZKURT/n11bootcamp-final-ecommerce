package com.yabozkurt.n11bootcamp.ecommerce.product.application.service.impl;

import com.yabozkurt.n11bootcamp.ecommerce.product.application.service.CategoryService;
import com.yabozkurt.n11bootcamp.ecommerce.product.domain.exception.CategoryNotFoundException;
import com.yabozkurt.n11bootcamp.ecommerce.product.domain.model.Category;
import com.yabozkurt.n11bootcamp.ecommerce.product.domain.repository.CategoryRepository;
import com.yabozkurt.n11bootcamp.ecommerce.product.presentation.dto.request.CategoryRequest;
import com.yabozkurt.n11bootcamp.ecommerce.product.presentation.dto.response.CategoryResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class CategoryServiceImpl implements CategoryService {

    private final CategoryRepository categoryRepository;

    public CategoryServiceImpl(CategoryRepository categoryRepository) {
        this.categoryRepository = categoryRepository;
    }

    @Override
    public List<CategoryResponse> getAll() {
        return categoryRepository.findAll().stream().map(c -> CategoryServiceImpl.toResponse(c)).toList();
    }

    @Override
    public CategoryResponse getById(Long id) {
        return toResponse(findById(id));
    }

    @Override
    public List<CategoryResponse> getRootCategories() {
        return categoryRepository.findByParentIsNull().stream().map(c -> CategoryServiceImpl.toResponse(c)).toList();
    }

    @Override
    public List<CategoryResponse> getChildren(Long parentId) {
        return categoryRepository.findByParentId(parentId).stream().map(c -> CategoryServiceImpl.toResponse(c)).toList();
    }

    @Override
    @Transactional
    public CategoryResponse create(CategoryRequest request) {
        if (categoryRepository.existsByName(request.getName())) {
            throw new IllegalStateException("Category already exists: " + request.getName());
        }
        Category parent = request.getParentId() != null ? findById(request.getParentId()) : null;
        Category category = new Category(request.getName(), parent);
        return toResponse(categoryRepository.save(category));
    }

    @Override
    @Transactional
    public CategoryResponse update(Long id, CategoryRequest request) {
        Category category = findById(id);
        category.setName(request.getName());
        if (request.getParentId() != null) {
            category.setParent(findById(request.getParentId()));
        } else {
            category.setParent(null);
        }
        return toResponse(categoryRepository.save(category));
    }

    @Override
    @Transactional
    public void delete(Long id) {
        findById(id);
        categoryRepository.deleteById(id);
    }

    private Category findById(Long id) {
        return categoryRepository.findById(id).orElseThrow(() -> new CategoryNotFoundException(id));
    }

    public static CategoryResponse toResponse(Category category) {
        CategoryResponse res = new CategoryResponse();
        res.setId(category.getId());
        res.setName(category.getName());
        if (category.getParent() != null) {
            res.setParentId(category.getParent().getId());
            res.setParentName(category.getParent().getName());
        }
        return res;
    }
}
