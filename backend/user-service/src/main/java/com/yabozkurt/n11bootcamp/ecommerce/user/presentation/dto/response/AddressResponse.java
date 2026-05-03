package com.yabozkurt.n11bootcamp.ecommerce.user.presentation.dto.response;

public class AddressResponse {

    private Long id;
    private String title;
    private String recipientName;
    private String recipientPhone;
    private String city;
    private String district;
    private String neighborhood;
    private String addressLine;
    private String zipCode;
    private boolean isDefault;

    public AddressResponse(Long id, String title, String recipientName, String recipientPhone,
                           String city, String district, String neighborhood,
                           String addressLine, String zipCode, boolean isDefault) {
        this.id = id;
        this.title = title;
        this.recipientName = recipientName;
        this.recipientPhone = recipientPhone;
        this.city = city;
        this.district = district;
        this.neighborhood = neighborhood;
        this.addressLine = addressLine;
        this.zipCode = zipCode;
        this.isDefault = isDefault;
    }

    public Long getId() { return id; }
    public String getTitle() { return title; }
    public String getRecipientName() { return recipientName; }
    public String getRecipientPhone() { return recipientPhone; }
    public String getCity() { return city; }
    public String getDistrict() { return district; }
    public String getNeighborhood() { return neighborhood; }
    public String getAddressLine() { return addressLine; }
    public String getZipCode() { return zipCode; }
    public boolean isDefault() { return isDefault; }
}
