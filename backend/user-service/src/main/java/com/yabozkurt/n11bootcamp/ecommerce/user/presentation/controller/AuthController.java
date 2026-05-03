package com.yabozkurt.n11bootcamp.ecommerce.user.presentation.controller;

import com.yabozkurt.n11bootcamp.ecommerce.user.application.dto.AuthResult;
import com.yabozkurt.n11bootcamp.ecommerce.user.application.service.AuthService;
import com.yabozkurt.n11bootcamp.ecommerce.user.infrastructure.web.CookieService;
import com.yabozkurt.n11bootcamp.ecommerce.user.presentation.dto.request.ForgotPasswordRequest;
import com.yabozkurt.n11bootcamp.ecommerce.user.presentation.dto.request.LoginRequest;
import com.yabozkurt.n11bootcamp.ecommerce.user.presentation.dto.request.RegisterRequest;
import com.yabozkurt.n11bootcamp.ecommerce.user.presentation.dto.request.ResetPasswordRequest;
import com.yabozkurt.n11bootcamp.ecommerce.user.presentation.dto.response.ApiResponse;
import com.yabozkurt.n11bootcamp.ecommerce.user.presentation.dto.response.AuthResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Auth", description = "Authentication operations")
@RestController
@RequestMapping("/api/users/auth")
public class AuthController {

    private final AuthService authService;
    private final CookieService cookieService;

    public AuthController(AuthService authService, CookieService cookieService) {
        this.authService = authService;
        this.cookieService = cookieService;
    }

    @Operation(summary = "Register", description = "Creates a new user and returns tokens as cookies")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Registration successful"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "Email already in use")
    })
    @PostMapping("/register")
    public ResponseEntity<ApiResponse<AuthResponse>> register(@Valid @RequestBody RegisterRequest request,
                                                               HttpServletResponse response) {
        AuthResult result = authService.register(request);
        cookieService.setAuthCookies(response, result.getAccessToken(), result.getRefreshToken());
        return ResponseEntity.ok(ApiResponse.ok("Registration successful", toResponse(result)));
    }

    @Operation(summary = "Login", description = "Authenticates user and returns tokens as cookies")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Login successful"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Invalid email or password")
    })
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AuthResponse>> login(@Valid @RequestBody LoginRequest request,
                                                            HttpServletResponse response) {
        AuthResult result = authService.login(request);
        cookieService.setAuthCookies(response, result.getAccessToken(), result.getRefreshToken());
        return ResponseEntity.ok(ApiResponse.ok("Login successful", toResponse(result)));
    }

    @Operation(summary = "Refresh token", description = "Creates a new access token using refresh token cookie")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Token refreshed"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Refresh token is invalid or expired")
    })
    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<Void>> refresh(
            @CookieValue(name = "refresh_token", required = false) String refreshToken,
            HttpServletResponse response) {

        if (refreshToken == null) {
            return ResponseEntity.status(401).body(ApiResponse.error("Refresh token not found"));
        }

        AuthResult result = authService.refresh(refreshToken);
        cookieService.setAuthCookies(response, result.getAccessToken(), result.getRefreshToken());
        return ResponseEntity.ok(ApiResponse.ok("Token refreshed", null));
    }

    @Operation(summary = "Logout", description = "Invalidates tokens and clears auth cookies")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Logout successful")
    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout(
            @CookieValue(name = "access_token", required = false) String accessToken,
            @CookieValue(name = "refresh_token", required = false) String refreshToken,
            HttpServletResponse response) {

        authService.logout(accessToken, refreshToken);
        cookieService.clearAuthCookies(response);
        return ResponseEntity.ok(ApiResponse.ok("Logout successful", null));
    }

    @Operation(summary = "Forgot password", description = "Sends password reset token by email (always returns 200 to prevent user enumeration)")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Request received")
    @PostMapping("/forgot-password")
    public ResponseEntity<ApiResponse<Void>> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        authService.forgotPassword(request);
        return ResponseEntity.ok(ApiResponse.ok("Password reset request received", null));
    }

    @Operation(summary = "Reset password", description = "Sets a new password using a valid reset token")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Password updated"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Token is invalid or expired")
    })
    @PostMapping("/reset-password")
    public ResponseEntity<ApiResponse<Void>> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        authService.resetPassword(request);
        return ResponseEntity.ok(ApiResponse.ok("Password updated successfully", null));
    }

    private AuthResponse toResponse(AuthResult result) {
        return new AuthResponse(
                result.getEmail(),
                result.getFirstName(),
                result.getLastName(),
                result.getRole()
        );
    }
}
