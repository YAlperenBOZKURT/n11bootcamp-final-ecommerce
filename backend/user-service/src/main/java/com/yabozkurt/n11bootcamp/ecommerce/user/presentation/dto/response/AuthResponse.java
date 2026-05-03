package com.yabozkurt.n11bootcamp.ecommerce.user.presentation.dto.response;

public class AuthResponse {

    private final String email;
    private final String firstName;
    private final String lastName;
    private final String role;

    public AuthResponse(String email, String firstName, String lastName, String role) {
        this.email = email;
        this.firstName = firstName;
        this.lastName = lastName;
        this.role = role;
    }

    public String getEmail() { return email; }
    public String getFirstName() { return firstName; }
    public String getLastName() { return lastName; }
    public String getRole() { return role; }
}
