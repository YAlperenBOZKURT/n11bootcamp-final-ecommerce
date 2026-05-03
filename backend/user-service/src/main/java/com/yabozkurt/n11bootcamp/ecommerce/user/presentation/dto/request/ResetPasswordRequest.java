package com.yabozkurt.n11bootcamp.ecommerce.user.presentation.dto.request;

import com.yabozkurt.n11bootcamp.ecommerce.user.presentation.dto.validation.StrongPassword;
import jakarta.validation.constraints.NotBlank;

public class ResetPasswordRequest {

    @NotBlank
    private String token;

    @NotBlank
    @StrongPassword
    private String newPassword;

    public String getToken() { return token; }
    public void setToken(String token) { this.token = token; }

    public String getNewPassword() { return newPassword; }
    public void setNewPassword(String newPassword) { this.newPassword = newPassword; }
}
