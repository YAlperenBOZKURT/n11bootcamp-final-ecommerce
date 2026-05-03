package com.yabozkurt.n11bootcamp.ecommerce.user.application.service;

import com.yabozkurt.n11bootcamp.ecommerce.user.presentation.dto.request.AddressRequest;
import com.yabozkurt.n11bootcamp.ecommerce.user.presentation.dto.response.AddressResponse;

import java.util.List;

public interface AddressService {

    List<AddressResponse> getAddresses(Long userId);

    AddressResponse createAddress(Long userId, AddressRequest request);

    AddressResponse updateAddress(Long userId, Long addressId, AddressRequest request);

    void deleteAddress(Long userId, Long addressId);

    AddressResponse setDefaultAddress(Long userId, Long addressId);
}
