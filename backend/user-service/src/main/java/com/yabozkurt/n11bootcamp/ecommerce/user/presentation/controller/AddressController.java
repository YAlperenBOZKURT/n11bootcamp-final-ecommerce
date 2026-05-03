package com.yabozkurt.n11bootcamp.ecommerce.user.presentation.controller;

import com.yabozkurt.n11bootcamp.ecommerce.user.application.service.AddressService;
import com.yabozkurt.n11bootcamp.ecommerce.user.application.service.UserService;
import com.yabozkurt.n11bootcamp.ecommerce.user.presentation.dto.request.AddressRequest;
import com.yabozkurt.n11bootcamp.ecommerce.user.presentation.dto.response.AddressResponse;
import com.yabozkurt.n11bootcamp.ecommerce.user.presentation.dto.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Addresses", description = "Address management")
@SecurityRequirement(name = "cookieAuth")
@RestController
@RequestMapping("/api/users/me/addresses")
public class AddressController {

    private final AddressService addressService;
    private final UserService userService;

    public AddressController(AddressService addressService, UserService userService) {
        this.addressService = addressService;
        this.userService = userService;
    }

    @Operation(summary = "List addresses")
    @GetMapping
    public ResponseEntity<ApiResponse<List<AddressResponse>>> getAddresses(
            @AuthenticationPrincipal String email) {
        Long userId = userService.getProfile(email).getId();
        return ResponseEntity.ok(ApiResponse.ok(addressService.getAddresses(userId)));
    }

    @Operation(summary = "Create address")
    @PostMapping
    public ResponseEntity<ApiResponse<AddressResponse>> createAddress(
            @AuthenticationPrincipal String email,
            @Valid @RequestBody AddressRequest request) {
        Long userId = userService.getProfile(email).getId();
        return ResponseEntity.ok(ApiResponse.ok(addressService.createAddress(userId, request)));
    }

    @Operation(summary = "Update address")
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<AddressResponse>> updateAddress(
            @AuthenticationPrincipal String email,
            @PathVariable Long id,
            @Valid @RequestBody AddressRequest request) {
        Long userId = userService.getProfile(email).getId();
        return ResponseEntity.ok(ApiResponse.ok(addressService.updateAddress(userId, id, request)));
    }

    @Operation(summary = "Delete address")
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteAddress(
            @AuthenticationPrincipal String email,
            @PathVariable Long id) {
        Long userId = userService.getProfile(email).getId();
        addressService.deleteAddress(userId, id);
        return ResponseEntity.ok(ApiResponse.ok("Address deleted", null));
    }

    @Operation(summary = "Set default address")
    @PutMapping("/{id}/default")
    public ResponseEntity<ApiResponse<AddressResponse>> setDefault(
            @AuthenticationPrincipal String email,
            @PathVariable Long id) {
        Long userId = userService.getProfile(email).getId();
        return ResponseEntity.ok(ApiResponse.ok(addressService.setDefaultAddress(userId, id)));
    }
}
