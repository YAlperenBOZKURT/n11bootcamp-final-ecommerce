package com.yabozkurt.n11bootcamp.ecommerce.product.domain.repository;

import com.yabozkurt.n11bootcamp.ecommerce.product.domain.model.Product;
import com.yabozkurt.n11bootcamp.ecommerce.product.domain.model.enums.ProductStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ProductRepository extends JpaRepository<Product, Long> {
    Page<Product> findByStatus(ProductStatus status, Pageable pageable);
    Page<Product> findByCategoryIdAndStatus(Long categoryId, ProductStatus status, Pageable pageable);
    Optional<Product> findByIdAndStatusNot(Long id, ProductStatus status);

    @Query("select p.id from Product p where p.status = :status order by p.createdAt desc")
    Page<Long> findIdsByStatus(@Param("status") ProductStatus status, Pageable pageable);

    @Query("select p.id from Product p where p.category.id = :categoryId and p.status = :status order by p.createdAt desc")
    Page<Long> findIdsByCategoryIdAndStatus(@Param("categoryId") Long categoryId,
                                            @Param("status") ProductStatus status,
                                            Pageable pageable);

    @Query("""
            select distinct p from Product p
            left join fetch p.variants
            left join fetch p.category
            where p.id in :ids
            """)
    List<Product> findAllByIdInWithCategoryAndVariants(@Param("ids") List<Long> ids);

    @Query("""
            select distinct p from Product p
            left join fetch p.variants
            left join fetch p.category
            """)
    List<Product> findAllWithCategoryAndVariants();
}
