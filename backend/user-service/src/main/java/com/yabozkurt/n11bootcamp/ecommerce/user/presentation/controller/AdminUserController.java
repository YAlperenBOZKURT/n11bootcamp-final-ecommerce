package com.yabozkurt.n11bootcamp.ecommerce.user.presentation.controller;

import com.yabozkurt.n11bootcamp.ecommerce.user.application.service.UserService;
import com.yabozkurt.n11bootcamp.ecommerce.user.presentation.dto.response.ApiResponse;
import com.yabozkurt.n11bootcamp.ecommerce.user.presentation.dto.response.UserResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Users", description = "User management - ADMIN only")
@SecurityRequirement(name = "cookieAuth")
@RestController
@RequestMapping("/api/users/admin")
@PreAuthorize("hasRole('ADMIN')")
public class AdminUserController {

    private final UserService userService;

    public AdminUserController(UserService userService) {
        this.userService = userService;
    }

    @Operation(summary = "Get user by ID")
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<UserResponse>> getUserById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok(userService.getUserById(id)));
    }

    @Operation(summary = "List all users")
    @GetMapping
    public ResponseEntity<ApiResponse<Page<UserResponse>>> getAllUsers(
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.ok(userService.getAllUsers(pageable)));
    }

    @Operation(summary = "Freeze user")
    @PatchMapping("/{id}/freeze")
    public ResponseEntity<ApiResponse<Void>> freezeUser(@PathVariable Long id) {
        userService.freezeUser(id);
        return ResponseEntity.ok(ApiResponse.ok("User frozen", null));
    }

    @Operation(summary = "Unfreeze user")
    @PatchMapping("/{id}/unfreeze")
    public ResponseEntity<ApiResponse<Void>> unfreezeUser(@PathVariable Long id) {
        userService.unfreezeUser(id);
        return ResponseEntity.ok(ApiResponse.ok("User activated", null));
    }

    @Operation(summary = "Delete user (soft delete)")
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteUserByAdmin(@PathVariable Long id) {
        userService.deleteUserByAdmin(id);
        return ResponseEntity.ok(ApiResponse.ok("User deleted", null));
    }
}
