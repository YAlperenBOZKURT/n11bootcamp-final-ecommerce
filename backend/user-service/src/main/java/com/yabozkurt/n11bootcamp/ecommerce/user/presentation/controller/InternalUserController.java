package com.yabozkurt.n11bootcamp.ecommerce.user.presentation.controller;

import com.yabozkurt.n11bootcamp.ecommerce.user.application.service.UserService;
import com.yabozkurt.n11bootcamp.ecommerce.user.presentation.dto.response.ApiResponse;
import com.yabozkurt.n11bootcamp.ecommerce.user.presentation.dto.response.InternalUserResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Internal", description = "Inter-service communication - not publicly accessible")
@RestController
@RequestMapping("/api/users/internal")
public class InternalUserController {

    private final UserService userService;

    public InternalUserController(UserService userService) {
        this.userService = userService;
    }

    @Operation(summary = "User and default address details")
    @GetMapping("/{userId}")
    public ResponseEntity<ApiResponse<InternalUserResponse>> getInternalUser(@PathVariable Long userId) {
        return ResponseEntity.ok(ApiResponse.ok(userService.getInternalUser(userId)));
    }
}
