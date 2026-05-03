package com.yabozkurt.n11bootcamp.ecommerce.payment.domain.repository;

import com.yabozkurt.n11bootcamp.ecommerce.payment.domain.model.PaymentTransaction;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PaymentTransactionRepository extends JpaRepository<PaymentTransaction, Long> {
    Optional<PaymentTransaction> findByOrderId(String orderId);
}
