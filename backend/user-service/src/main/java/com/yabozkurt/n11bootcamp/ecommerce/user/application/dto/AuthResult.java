package com.yabozkurt.n11bootcamp.ecommerce.user.application.dto;

public class AuthResult {

    private final String accessToken;
    private final String refreshToken;
    private final String email;
    private final String firstName;
    private final String lastName;
    private final String role;

    public AuthResult(String accessToken, String refreshToken,
                      String email, String firstName, String lastName, String role) {
        this.accessToken = accessToken;
        this.refreshToken = refreshToken;
        this.email = email;
        this.firstName = firstName;
        this.lastName = lastName;
        this.role = role;
    }

    public String getAccessToken() { return accessToken; }
    public String getRefreshToken() { return refreshToken; }
    public String getEmail() { return email; }
    public String getFirstName() { return firstName; }
    public String getLastName() { return lastName; }
    public String getRole() { return role; }
}
