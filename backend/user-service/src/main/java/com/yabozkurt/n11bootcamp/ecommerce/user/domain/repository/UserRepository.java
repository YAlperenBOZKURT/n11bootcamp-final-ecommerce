package com.yabozkurt.n11bootcamp.ecommerce.user.domain.repository;

import com.yabozkurt.n11bootcamp.ecommerce.user.domain.model.User;
import com.yabozkurt.n11bootcamp.ecommerce.user.domain.model.enums.UserStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);
    boolean existsByPhoneNumber(String phoneNumber);

    boolean existsByEmailAndStatusNot(String email, UserStatus status);
    boolean existsByPhoneNumberAndStatusNot(String phoneNumber, UserStatus status);
    boolean existsByPhoneNumberAndIdNotAndStatusNot(String phoneNumber, Long id, UserStatus status);

    Optional<User> findByEmailAndStatusNot(String email, UserStatus status);

    Optional<User> findByIdAndStatusNot(Long id, UserStatus status);

    Page<User> findAllByStatusNot(UserStatus status, Pageable pageable);
}
