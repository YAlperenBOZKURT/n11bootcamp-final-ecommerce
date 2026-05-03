package com.yabozkurt.n11bootcamp.ecommerce.user.application.service;

import com.yabozkurt.n11bootcamp.ecommerce.user.application.dto.AuthResult;
import com.yabozkurt.n11bootcamp.ecommerce.user.presentation.dto.request.ForgotPasswordRequest;
import com.yabozkurt.n11bootcamp.ecommerce.user.presentation.dto.request.LoginRequest;
import com.yabozkurt.n11bootcamp.ecommerce.user.presentation.dto.request.RegisterRequest;
import com.yabozkurt.n11bootcamp.ecommerce.user.presentation.dto.request.ResetPasswordRequest;

public interface AuthService {

    AuthResult register(RegisterRequest request);

    AuthResult login(LoginRequest request);

    AuthResult refresh(String refreshToken);

    void logout(String accessToken, String refreshToken);

    void forgotPassword(ForgotPasswordRequest request);

    void resetPassword(ResetPasswordRequest request);
}
