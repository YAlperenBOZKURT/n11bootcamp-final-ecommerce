package com.yabozkurt.n11bootcamp.ecommerce.user.application.service.impl;

import com.yabozkurt.n11bootcamp.ecommerce.user.application.service.AddressService;
import com.yabozkurt.n11bootcamp.ecommerce.user.domain.exception.AddressNotFoundException;
import com.yabozkurt.n11bootcamp.ecommerce.user.domain.model.Address;
import com.yabozkurt.n11bootcamp.ecommerce.user.domain.repository.AddressRepository;
import com.yabozkurt.n11bootcamp.ecommerce.user.presentation.dto.request.AddressRequest;
import com.yabozkurt.n11bootcamp.ecommerce.user.presentation.dto.response.AddressResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class AddressServiceImpl implements AddressService {

    private static final int MAX_ADDRESSES = 10;

    private final AddressRepository addressRepository;

    public AddressServiceImpl(AddressRepository addressRepository) {
        this.addressRepository = addressRepository;
    }

    @Override
    public List<AddressResponse> getAddresses(Long userId) {
        return addressRepository.findByUserIdOrderByIsDefaultDescCreatedAtDesc(userId)
                .stream()
                .map(AddressServiceImpl::toResponse)
                .toList();
    }

    @Override
    @Transactional
    public AddressResponse createAddress(Long userId, AddressRequest request) {
        int count = addressRepository.countByUserId(userId);
        if (count >= MAX_ADDRESSES) {
            throw new IllegalStateException("En fazla " + MAX_ADDRESSES + " adres ekleyebilirsiniz");
        }

        Address address = toEntity(request);
        address.setUserId(userId);
        address.setDefault(count == 0);

        return toResponse(addressRepository.save(address));
    }

    @Override
    @Transactional
    public AddressResponse updateAddress(Long userId, Long addressId, AddressRequest request) {
        Address address = addressRepository.findByIdAndUserId(addressId, userId)
                .orElseThrow(() -> new AddressNotFoundException(addressId));

        address.setTitle(request.getTitle());
        address.setRecipientName(request.getRecipientName());
        address.setRecipientPhone(request.getRecipientPhone());
        address.setCity(request.getCity());
        address.setDistrict(request.getDistrict());
        address.setNeighborhood(request.getNeighborhood());
        address.setAddressLine(request.getAddressLine());
        address.setZipCode(request.getZipCode());

        return toResponse(addressRepository.save(address));
    }

    @Override
    @Transactional
    public void deleteAddress(Long userId, Long addressId) {
        Address address = addressRepository.findByIdAndUserId(addressId, userId)
                .orElseThrow(() -> new AddressNotFoundException(addressId));

        addressRepository.delete(address);

        // If deleted address was default, promote the most recently created one, maybe its wrong i don't know !!!
        if (address.isDefault()) {
            addressRepository.findByUserIdOrderByIsDefaultDescCreatedAtDesc(userId)
                    .stream()
                    .findFirst()
                    .ifPresent(next -> {
                        next.setDefault(true);
                        addressRepository.save(next);
                    });
        }
    }

    @Override
    @Transactional
    public AddressResponse setDefaultAddress(Long userId, Long addressId) {
        Address address = addressRepository.findByIdAndUserId(addressId, userId)
                .orElseThrow(() -> new AddressNotFoundException(addressId));

        addressRepository.clearDefaultByUserId(userId);
        address.setDefault(true);

        return toResponse(addressRepository.save(address));
    }

    private Address toEntity(AddressRequest request) {
        Address address = new Address();
        address.setTitle(request.getTitle());
        address.setRecipientName(request.getRecipientName());
        address.setRecipientPhone(request.getRecipientPhone());
        address.setCity(request.getCity());
        address.setDistrict(request.getDistrict());
        address.setNeighborhood(request.getNeighborhood());
        address.setAddressLine(request.getAddressLine());
        address.setZipCode(request.getZipCode());
        return address;
    }

    public static AddressResponse toResponse(Address address) {
        return new AddressResponse(
                address.getId(),
                address.getTitle(),
                address.getRecipientName(),
                address.getRecipientPhone(),
                address.getCity(),
                address.getDistrict(),
                address.getNeighborhood(),
                address.getAddressLine(),
                address.getZipCode(),
                address.isDefault()
        );
    }
}
