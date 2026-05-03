package com.yabozkurt.n11bootcamp.ecommerce.user.application.service;

import com.yabozkurt.n11bootcamp.ecommerce.user.presentation.dto.request.UpdateProfileRequest;
import com.yabozkurt.n11bootcamp.ecommerce.user.presentation.dto.response.InternalUserResponse;
import com.yabozkurt.n11bootcamp.ecommerce.user.presentation.dto.response.UserResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface UserService {

    UserResponse getProfile(String email);

    UserResponse updateProfile(String email, UpdateProfileRequest request);

    void changePassword(String email, String currentPassword, String newPassword);

    void deleteOwnAccount(String email);

    InternalUserResponse getInternalUser(Long userId);

    UserResponse getUserById(Long id);

    Page<UserResponse> getAllUsers(Pageable pageable);

    void freezeUser(Long id);

    void unfreezeUser(Long id);

    void deleteUserByAdmin(Long id);
}
