package com.yabozkurt.n11bootcamp.ecommerce.user.presentation.controller;

import com.yabozkurt.n11bootcamp.ecommerce.user.application.service.UserService;
import com.yabozkurt.n11bootcamp.ecommerce.user.presentation.dto.request.ChangePasswordRequest;
import com.yabozkurt.n11bootcamp.ecommerce.user.presentation.dto.request.UpdateProfileRequest;
import com.yabozkurt.n11bootcamp.ecommerce.user.presentation.dto.response.ApiResponse;
import com.yabozkurt.n11bootcamp.ecommerce.user.presentation.dto.response.UserResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Profile", description = "Own profile operations")
@SecurityRequirement(name = "cookieAuth")
@RestController
@RequestMapping("/api/users/me")
public class ProfileController {

    private final UserService userService;

    public ProfileController(UserService userService) {
        this.userService = userService;
    }

    @Operation(summary = "Get profile")
    @GetMapping
    public ResponseEntity<ApiResponse<UserResponse>> getProfile(@AuthenticationPrincipal String email) {
        return ResponseEntity.ok(ApiResponse.ok(userService.getProfile(email)));
    }

    @Operation(summary = "Update profile")
    @PutMapping
    public ResponseEntity<ApiResponse<UserResponse>> updateProfile(
            @AuthenticationPrincipal String email,
            @Valid @RequestBody UpdateProfileRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(userService.updateProfile(email, request)));
    }

    @Operation(summary = "Change password")
    @PutMapping("/password")
    public ResponseEntity<ApiResponse<Void>> changePassword(
            @AuthenticationPrincipal String email,
            @Valid @RequestBody ChangePasswordRequest request) {
        userService.changePassword(email, request.getCurrentPassword(), request.getNewPassword());
        return ResponseEntity.ok(ApiResponse.ok("Password changed", null));
    }

    @Operation(summary = "Delete account")
    @DeleteMapping
    public ResponseEntity<ApiResponse<Void>> deleteOwnAccount(@AuthenticationPrincipal String email) {
        userService.deleteOwnAccount(email);
        return ResponseEntity.ok(ApiResponse.ok("Account deleted", null));
    }
}
