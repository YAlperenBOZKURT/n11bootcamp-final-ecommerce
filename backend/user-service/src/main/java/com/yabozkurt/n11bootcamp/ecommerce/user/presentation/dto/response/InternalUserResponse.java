package com.yabozkurt.n11bootcamp.ecommerce.user.presentation.dto.response;

public class InternalUserResponse {

    private Long id;
    private String email;
    private String firstName;
    private String lastName;
    private String phoneNumber;
    private String role;
    private String status;
    private AddressResponse defaultAddress;

    public InternalUserResponse(Long id, String email, String firstName, String lastName,
                                String phoneNumber, String role, String status,
                                AddressResponse defaultAddress) {
        this.id = id;
        this.email = email;
        this.firstName = firstName;
        this.lastName = lastName;
        this.phoneNumber = phoneNumber;
        this.role = role;
        this.status = status;
        this.defaultAddress = defaultAddress;
    }

    public Long getId() { return id; }
    public String getEmail() { return email; }
    public String getFirstName() { return firstName; }
    public String getLastName() { return lastName; }
    public String getPhoneNumber() { return phoneNumber; }
    public String getRole() { return role; }
    public String getStatus() { return status; }
    public AddressResponse getDefaultAddress() { return defaultAddress; }
}
