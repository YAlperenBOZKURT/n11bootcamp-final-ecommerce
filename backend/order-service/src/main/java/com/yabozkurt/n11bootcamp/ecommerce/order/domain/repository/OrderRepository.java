package com.yabozkurt.n11bootcamp.ecommerce.order.domain.repository;

import com.yabozkurt.n11bootcamp.ecommerce.order.domain.model.Order;
import com.yabozkurt.n11bootcamp.ecommerce.order.domain.model.enums.OrderStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface OrderRepository extends JpaRepository<Order, Long> {

    Optional<Order> findByOrderNumber(String orderNumber);

    List<Order> findByUserId(Long userId);

    @Query("select distinct o from Order o left join fetch o.items where o.userId = :userId order by o.createdAt desc")
    List<Order> findByUserIdWithItems(@Param("userId") Long userId);

    @Query("select o.id from Order o order by o.createdAt desc")
    Page<Long> findAllOrderIds(Pageable pageable);

    @Query("select distinct o from Order o left join fetch o.items where o.id in :ids")
    List<Order> findAllByIdInWithItems(@Param("ids") List<Long> ids);

    Page<Order> findByUserId(Long userId, Pageable pageable);

    Page<Order> findByUserIdAndStatus(Long userId, OrderStatus status, Pageable pageable);
}
