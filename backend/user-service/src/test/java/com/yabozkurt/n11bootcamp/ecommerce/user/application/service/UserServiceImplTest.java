package com.yabozkurt.n11bootcamp.ecommerce.user.application.service;

import com.yabozkurt.n11bootcamp.ecommerce.user.application.service.impl.UserServiceImpl;
import com.yabozkurt.n11bootcamp.ecommerce.user.domain.exception.UserNotFoundException;
import com.yabozkurt.n11bootcamp.ecommerce.user.domain.model.Address;
import com.yabozkurt.n11bootcamp.ecommerce.user.domain.model.User;
import com.yabozkurt.n11bootcamp.ecommerce.user.domain.model.enums.Role;
import com.yabozkurt.n11bootcamp.ecommerce.user.domain.model.enums.UserStatus;
import com.yabozkurt.n11bootcamp.ecommerce.user.domain.repository.AddressRepository;
import com.yabozkurt.n11bootcamp.ecommerce.user.domain.repository.UserRepository;
import com.yabozkurt.n11bootcamp.ecommerce.user.presentation.dto.request.UpdateProfileRequest;
import com.yabozkurt.n11bootcamp.ecommerce.user.presentation.dto.response.InternalUserResponse;
import com.yabozkurt.n11bootcamp.ecommerce.user.presentation.dto.response.UserResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceImplTest {

    @Mock UserRepository userRepository;
    @Mock AddressRepository addressRepository;
    @Mock PasswordEncoder passwordEncoder;

    @InjectMocks UserServiceImpl userService;

    private User activeUser;

    @BeforeEach
    void setUp() {
        activeUser = new User("ali@test.com", "encoded", "Ali", "Veli", "5551234567", Role.CUSTOMER);
        activeUser.setId(1L);
        activeUser.setStatus(UserStatus.ACTIVE);
    }

    // -- getProfile ------------------------------------------------------------

    @Test
    void getProfile_existingUser_returnsResponse() {
        when(userRepository.findByEmailAndStatusNot("ali@test.com", UserStatus.DELETED))
                .thenReturn(Optional.of(activeUser));

        UserResponse response = userService.getProfile("ali@test.com");

        assertThat(response.getEmail()).isEqualTo("ali@test.com");
        assertThat(response.getFirstName()).isEqualTo("Ali");
    }

    @Test
    void getProfile_unknownEmail_throws() {
        when(userRepository.findByEmailAndStatusNot(any(), any())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.getProfile("ghost@test.com"))
                .isInstanceOf(UserNotFoundException.class);
    }

    // -- updateProfile ---------------------------------------------------------

    @Test
    void updateProfile_updatesFields() {
        when(userRepository.findByEmailAndStatusNot("ali@test.com", UserStatus.DELETED))
                .thenReturn(Optional.of(activeUser));
        when(userRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        UpdateProfileRequest req = new UpdateProfileRequest();
        req.setFirstName("Mehmet");
        req.setLastName("Kaya");
        req.setPhoneNumber("5559999999");

        UserResponse response = userService.updateProfile("ali@test.com", req);

        assertThat(response.getFirstName()).isEqualTo("Mehmet");
        assertThat(response.getLastName()).isEqualTo("Kaya");
        assertThat(response.getPhoneNumber()).isEqualTo("5559999999");
    }

    // -- changePassword --------------------------------------------------------

    @Test
    void changePassword_correctCurrent_succeeds() {
        when(userRepository.findByEmailAndStatusNot("ali@test.com", UserStatus.DELETED))
                .thenReturn(Optional.of(activeUser));
        when(passwordEncoder.matches("old123", "encoded")).thenReturn(true);
        when(passwordEncoder.encode("New123!")).thenReturn("newEncoded");

        assertThatNoException().isThrownBy(() ->
                userService.changePassword("ali@test.com", "old123", "New123!"));

        assertThat(activeUser.getPassword()).isEqualTo("newEncoded");
    }

    @Test
    void changePassword_wrongCurrent_throws() {
        when(userRepository.findByEmailAndStatusNot("ali@test.com", UserStatus.DELETED))
                .thenReturn(Optional.of(activeUser));
        when(passwordEncoder.matches(any(), any())).thenReturn(false);

        assertThatThrownBy(() -> userService.changePassword("ali@test.com", "wrong", "New123!"))
                .isInstanceOf(BadCredentialsException.class);
    }

    // -- deleteOwnAccount ------------------------------------------------------

    @Test
    void deleteOwnAccount_setsStatusDeleted() {
        when(userRepository.findByEmailAndStatusNot("ali@test.com", UserStatus.DELETED))
                .thenReturn(Optional.of(activeUser));

        userService.deleteOwnAccount("ali@test.com");

        assertThat(activeUser.getStatus()).isEqualTo(UserStatus.DELETED);
    }

    // -- getInternalUser -------------------------------------------------------

    @Test
    void getInternalUser_withDefaultAddress_returnsAddress() {
        when(userRepository.findByIdAndStatusNot(1L, UserStatus.DELETED))
                .thenReturn(Optional.of(activeUser));

        Address addr = new Address();
        addr.setId(10L);
        addr.setUserId(1L);
        addr.setTitle("Ev");
        addr.setRecipientName("Ali Veli");
        addr.setRecipientPhone("5551234567");
        addr.setCity("İstanbul");
        addr.setDistrict("Kadıköy");
        addr.setAddressLine("Test Sokak No:1");
        addr.setDefault(true);

        when(addressRepository.findByUserIdAndIsDefaultTrue(1L)).thenReturn(Optional.of(addr));

        InternalUserResponse response = userService.getInternalUser(1L);

        assertThat(response.getEmail()).isEqualTo("ali@test.com");
        assertThat(response.getDefaultAddress()).isNotNull();
        assertThat(response.getDefaultAddress().getCity()).isEqualTo("İstanbul");
    }

    @Test
    void getInternalUser_withoutAddress_defaultAddressIsNull() {
        when(userRepository.findByIdAndStatusNot(1L, UserStatus.DELETED))
                .thenReturn(Optional.of(activeUser));
        when(addressRepository.findByUserIdAndIsDefaultTrue(1L)).thenReturn(Optional.empty());

        InternalUserResponse response = userService.getInternalUser(1L);

        assertThat(response.getDefaultAddress()).isNull();
    }
}
