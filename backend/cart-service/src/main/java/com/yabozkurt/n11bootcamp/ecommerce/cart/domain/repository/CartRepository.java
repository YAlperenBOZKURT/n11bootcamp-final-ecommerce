package com.yabozkurt.n11bootcamp.ecommerce.cart.domain.repository;

import com.yabozkurt.n11bootcamp.ecommerce.cart.domain.model.Cart;
import org.springframework.data.repository.CrudRepository;

public interface CartRepository extends CrudRepository<Cart, Long> {
}
