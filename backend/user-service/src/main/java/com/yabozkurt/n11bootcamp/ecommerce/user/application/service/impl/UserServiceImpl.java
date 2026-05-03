package com.yabozkurt.n11bootcamp.ecommerce.user.application.service.impl;

import com.yabozkurt.n11bootcamp.ecommerce.user.application.service.UserService;
import com.yabozkurt.n11bootcamp.ecommerce.user.domain.exception.PhoneNumberAlreadyExistsException;
import com.yabozkurt.n11bootcamp.ecommerce.user.domain.exception.UserNotFoundException;
import com.yabozkurt.n11bootcamp.ecommerce.user.domain.model.User;
import com.yabozkurt.n11bootcamp.ecommerce.user.domain.model.enums.UserStatus;
import com.yabozkurt.n11bootcamp.ecommerce.user.domain.repository.AddressRepository;
import com.yabozkurt.n11bootcamp.ecommerce.user.domain.repository.UserRepository;
import com.yabozkurt.n11bootcamp.ecommerce.user.presentation.dto.request.UpdateProfileRequest;
import com.yabozkurt.n11bootcamp.ecommerce.user.presentation.dto.response.AddressResponse;
import com.yabozkurt.n11bootcamp.ecommerce.user.presentation.dto.response.InternalUserResponse;
import com.yabozkurt.n11bootcamp.ecommerce.user.presentation.dto.response.UserResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final AddressRepository addressRepository;
    private final PasswordEncoder passwordEncoder;

    public UserServiceImpl(UserRepository userRepository,
                           AddressRepository addressRepository,
                           PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.addressRepository = addressRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public UserResponse getProfile(String email) {
        User user = findActiveByEmail(email);
        return toResponse(user);
    }

    @Override
    @Transactional
    public UserResponse updateProfile(String email, UpdateProfileRequest request) {
        User user = findActiveByEmail(email);
        String normalizedPhone = normalizePhone(request.getPhoneNumber());
        if (normalizedPhone != null &&
                userRepository.existsByPhoneNumberAndIdNotAndStatusNot(normalizedPhone, user.getId(), UserStatus.DELETED)) {
            throw new PhoneNumberAlreadyExistsException(normalizedPhone);
        }
        user.setFirstName(request.getFirstName());
        user.setLastName(request.getLastName());
        user.setPhoneNumber(normalizedPhone);
        return toResponse(userRepository.save(user));
    }

    @Override
    @Transactional
    public void changePassword(String email, String currentPassword, String newPassword) {
        User user = findActiveByEmail(email);
        if (!passwordEncoder.matches(currentPassword, user.getPassword())) {
            throw new BadCredentialsException("Current password is incorrect");
        }
        user.setPassword(passwordEncoder.encode(newPassword));
    }

    @Override
    @Transactional
    public void deleteOwnAccount(String email) {
        User user = findActiveByEmail(email);
        user.setStatus(UserStatus.DELETED);
    }

    @Override
    public InternalUserResponse getInternalUser(Long userId) {
        User user = userRepository.findByIdAndStatusNot(userId, UserStatus.DELETED)
                .orElseThrow(() -> new UserNotFoundException(userId));

        AddressResponse defaultAddress = addressRepository.findByUserIdAndIsDefaultTrue(userId)
                .map(a -> AddressServiceImpl.toResponse(a))
                .orElse(null);

        return new InternalUserResponse(
                user.getId(),
                user.getEmail(),
                user.getFirstName(),
                user.getLastName(),
                user.getPhoneNumber(),
                user.getRole().name(),
                user.getStatus().name(),
                defaultAddress
        );
    }

    // -- Admin operations -----------------------------------------------------

    @Override
    public UserResponse getUserById(Long id) {
        User user = userRepository.findByIdAndStatusNot(id, UserStatus.DELETED)
                .orElseThrow(() -> new UserNotFoundException(id));
        return toResponse(user);
    }

    @Override
    public Page<UserResponse> getAllUsers(Pageable pageable) {
        return userRepository.findAllByStatusNot(UserStatus.DELETED, pageable).map(this::toResponse);
    }

    @Override
    @Transactional
    public void freezeUser(Long id) {
        User user = userRepository.findByIdAndStatusNot(id, UserStatus.DELETED)
                .orElseThrow(() -> new UserNotFoundException(id));
        user.setStatus(UserStatus.FROZEN);
    }

    @Override
    @Transactional
    public void unfreezeUser(Long id) {
        User user = userRepository.findByIdAndStatusNot(id, UserStatus.DELETED)
                .orElseThrow(() -> new UserNotFoundException(id));
        user.setStatus(UserStatus.ACTIVE);
    }

    @Override
    @Transactional
    public void deleteUserByAdmin(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new UserNotFoundException(id));
        userRepository.delete(user);
    }

    // -- helpers ---------------------------------------------------------------

    private User findActiveByEmail(String email) {
        return userRepository.findByEmailAndStatusNot(email, UserStatus.DELETED)
                .orElseThrow(() -> new UserNotFoundException(email));
    }

    private UserResponse toResponse(User user) {
        return new UserResponse(
                user.getId(),
                user.getEmail(),
                user.getFirstName(),
                user.getLastName(),
                user.getPhoneNumber(),
                user.getRole().name(),
                user.getStatus().name(),
                user.getCreatedAt()
        );
    }

    private String normalizePhone(String phoneNumber) {
        if (phoneNumber == null) return null;
        String trimmed = phoneNumber.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
