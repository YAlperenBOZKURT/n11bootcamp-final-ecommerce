package com.yabozkurt.n11bootcamp.ecommerce.product.domain.repository;

import com.yabozkurt.n11bootcamp.ecommerce.product.domain.model.Category;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CategoryRepository extends JpaRepository<Category, Long> {
    Optional<Category> findByName(String name);

    @Override
    @EntityGraph(attributePaths = "parent")
    List<Category> findAll();

    @EntityGraph(attributePaths = "parent")
    List<Category> findByParentIsNull();

    @EntityGraph(attributePaths = "parent")
    List<Category> findByParentId(Long parentId);

    boolean existsByName(String name);
}
