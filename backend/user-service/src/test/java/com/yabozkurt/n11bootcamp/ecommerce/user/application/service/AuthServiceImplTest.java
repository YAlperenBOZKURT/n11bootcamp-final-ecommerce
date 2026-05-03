package com.yabozkurt.n11bootcamp.ecommerce.user.application.service;

import com.yabozkurt.n11bootcamp.ecommerce.user.application.dto.AuthResult;
import com.yabozkurt.n11bootcamp.ecommerce.user.application.service.impl.AuthServiceImpl;
import com.yabozkurt.n11bootcamp.ecommerce.user.domain.exception.InvalidTokenException;
import com.yabozkurt.n11bootcamp.ecommerce.user.domain.exception.UserAlreadyExistsException;
import com.yabozkurt.n11bootcamp.ecommerce.user.domain.exception.UserNotFoundException;
import com.yabozkurt.n11bootcamp.ecommerce.user.domain.model.User;
import com.yabozkurt.n11bootcamp.ecommerce.user.domain.model.enums.Role;
import com.yabozkurt.n11bootcamp.ecommerce.user.domain.model.enums.UserStatus;
import com.yabozkurt.n11bootcamp.ecommerce.user.domain.repository.UserRepository;
import com.yabozkurt.n11bootcamp.ecommerce.user.infrastructure.security.jwt.JwtService;
import com.yabozkurt.n11bootcamp.ecommerce.user.infrastructure.messaging.publisher.UserEventPublisher;
import com.yabozkurt.n11bootcamp.ecommerce.user.presentation.dto.request.ForgotPasswordRequest;
import com.yabozkurt.n11bootcamp.ecommerce.user.presentation.dto.request.LoginRequest;
import com.yabozkurt.n11bootcamp.ecommerce.user.presentation.dto.request.RegisterRequest;
import com.yabozkurt.n11bootcamp.ecommerce.user.presentation.dto.request.ResetPasswordRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceImplTest {

    @Mock private UserRepository userRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private JwtService jwtService;
    @Mock private TokenService tokenService;
    @Mock private AuthenticationManager authenticationManager;
    @Mock private UserEventPublisher eventPublisher;

    @InjectMocks
    private AuthServiceImpl authService;

    private User user;

    @BeforeEach
    void setUp() {
        user = new User("test@mail.com", "encoded_pass", "Ali", "Veli", "5551234567", Role.CUSTOMER);
    }

    // --- register ------------------------------------------------------------

    @Test
    void register_success() {
        RegisterRequest request = registerRequest("test@mail.com", "Test123!", "Ali", "Veli");

        when(userRepository.findByEmail("test@mail.com")).thenReturn(Optional.empty());
        when(passwordEncoder.encode("Test123!")).thenReturn("encoded_pass");
        when(userRepository.save(any())).thenReturn(user);
        when(jwtService.generateAccessToken(any())).thenReturn("access_token");
        when(jwtService.generateRefreshToken(any())).thenReturn("refresh_token");

        AuthResult result = authService.register(request);

        assertThat(result.getEmail()).isEqualTo("test@mail.com");
        assertThat(result.getAccessToken()).isEqualTo("access_token");
        assertThat(result.getRefreshToken()).isEqualTo("refresh_token");
        assertThat(result.getRole()).isEqualTo("CUSTOMER");
        verify(userRepository).save(any(User.class));
        verify(eventPublisher).publishUserRegistered(any());
    }

    @Test
    void register_emailAlreadyExists_throwsException() {
        RegisterRequest request = registerRequest("test@mail.com", "Test123!", "Ali", "Veli");
        when(userRepository.findByEmail("test@mail.com")).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> authService.register(request))
                .isInstanceOf(UserAlreadyExistsException.class)
                .hasMessageContaining("test@mail.com");

        verify(userRepository, never()).save(any());
    }

    @Test
    void register_deletedUser_reactivatesAccount() {
        RegisterRequest request = registerRequest("test@mail.com", "Test123!", "Ali", "Veli");
        user.setStatus(UserStatus.DELETED);

        when(userRepository.findByEmail("test@mail.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.encode("Test123!")).thenReturn("encoded_pass");
        when(jwtService.generateAccessToken(any())).thenReturn("access_token");
        when(jwtService.generateRefreshToken(any())).thenReturn("refresh_token");

        AuthResult result = authService.register(request);

        assertThat(result.getEmail()).isEqualTo("test@mail.com");
        assertThat(user.getStatus()).isEqualTo(UserStatus.ACTIVE);
        verify(userRepository).save(user);
    }

    // --- login ---------------------------------------------------------------

    @Test
    void login_success() {
        LoginRequest request = new LoginRequest();
        request.setEmail("test@mail.com");
        request.setPassword("Test123!");

        when(userRepository.findByEmail("test@mail.com")).thenReturn(Optional.of(user));
        when(jwtService.generateAccessToken(any())).thenReturn("access_token");
        when(jwtService.generateRefreshToken(any())).thenReturn("refresh_token");

        AuthResult result = authService.login(request);

        assertThat(result.getEmail()).isEqualTo("test@mail.com");
        verify(authenticationManager).authenticate(any(UsernamePasswordAuthenticationToken.class));
    }

    @Test
    void login_badCredentials_throwsException() {
        LoginRequest request = new LoginRequest();
        request.setEmail("test@mail.com");
        request.setPassword("wrong");

        doThrow(new BadCredentialsException("Bad credentials"))
                .when(authenticationManager).authenticate(any());

        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(BadCredentialsException.class);
    }

    @Test
    void login_userNotFound_throwsException() {
        LoginRequest request = new LoginRequest();
        request.setEmail("notfound@mail.com");
        request.setPassword("Test123!");

        when(userRepository.findByEmail("notfound@mail.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(UserNotFoundException.class);
    }

    // --- refresh -------------------------------------------------------------

    @Test
    void refresh_success() {
        when(tokenService.isTokenBlacklisted("refresh_token")).thenReturn(false);
        when(jwtService.isRefreshToken("refresh_token")).thenReturn(true);
        when(jwtService.extractEmail("refresh_token")).thenReturn("test@mail.com");
        when(userRepository.findByEmail("test@mail.com")).thenReturn(Optional.of(user));
        when(jwtService.getRemainingTtlMs("refresh_token")).thenReturn(1000L);
        when(jwtService.generateAccessToken(any())).thenReturn("new_access");
        when(jwtService.generateRefreshToken(any())).thenReturn("new_refresh");

        AuthResult result = authService.refresh("refresh_token");

        assertThat(result.getAccessToken()).isEqualTo("new_access");
        verify(tokenService).blacklistToken(eq("refresh_token"), eq(1000L));
    }

    @Test
    void refresh_blacklistedToken_throwsException() {
        when(tokenService.isTokenBlacklisted("refresh_token")).thenReturn(true);

        assertThatThrownBy(() -> authService.refresh("refresh_token"))
                .isInstanceOf(InvalidTokenException.class);
    }

    @Test
    void refresh_wrongTokenType_throwsException() {
        when(tokenService.isTokenBlacklisted("access_token")).thenReturn(false);
        when(jwtService.isRefreshToken("access_token")).thenReturn(false);

        assertThatThrownBy(() -> authService.refresh("access_token"))
                .isInstanceOf(InvalidTokenException.class)
                .hasMessageContaining("Invalid token type");
    }

    // --- logout --------------------------------------------------------------

    @Test
    void logout_blacklistsBothTokens() {
        when(jwtService.getRemainingTtlMs("access_token")).thenReturn(500L);
        when(jwtService.getRemainingTtlMs("refresh_token")).thenReturn(1000L);

        authService.logout("access_token", "refresh_token");

        verify(tokenService).blacklistToken("access_token", 500L);
        verify(tokenService).blacklistToken("refresh_token", 1000L);
    }

    @Test
    void logout_nullTokens_doesNotThrow() {
        assertThatCode(() -> authService.logout(null, null))
                .doesNotThrowAnyException();

        verifyNoInteractions(tokenService);
    }

    // --- forgotPassword ------------------------------------------------------

    @Test
    void forgotPassword_activeUser_storesResetToken() {
        ForgotPasswordRequest request = new ForgotPasswordRequest();
        request.setEmail("test@mail.com");

        when(userRepository.findByEmail("test@mail.com")).thenReturn(Optional.of(user));

        authService.forgotPassword(request);

        verify(tokenService).storeResetToken(anyString(), eq("test@mail.com"), anyLong());
        verify(eventPublisher).publishPasswordResetRequested(any());
    }

    @Test
    void forgotPassword_unknownEmail_doesNothing() {
        ForgotPasswordRequest request = new ForgotPasswordRequest();
        request.setEmail("ghost@mail.com");

        when(userRepository.findByEmail("ghost@mail.com")).thenReturn(Optional.empty());

        assertThatCode(() -> authService.forgotPassword(request)).doesNotThrowAnyException();
        verifyNoInteractions(tokenService);
    }

    @Test
    void forgotPassword_frozenUser_doesNotStoreToken() {
        user.setStatus(UserStatus.FROZEN);
        ForgotPasswordRequest request = new ForgotPasswordRequest();
        request.setEmail("test@mail.com");

        when(userRepository.findByEmail("test@mail.com")).thenReturn(Optional.of(user));

        authService.forgotPassword(request);

        verify(tokenService, never()).storeResetToken(anyString(), anyString(), anyLong());
    }

    // --- resetPassword -------------------------------------------------------

    @Test
    void resetPassword_validToken_updatesPasswordAndDeletesToken() {
        ResetPasswordRequest request = new ResetPasswordRequest();
        request.setToken("valid-token");
        request.setNewPassword("NewPass123!");

        when(tokenService.getResetTokenEmail("valid-token")).thenReturn("test@mail.com");
        when(userRepository.findByEmail("test@mail.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.encode("NewPass123!")).thenReturn("encoded_new");

        authService.resetPassword(request);

        assertThat(user.getPassword()).isEqualTo("encoded_new");
        verify(userRepository).save(user);
        verify(tokenService).deleteResetToken("valid-token");
    }

    @Test
    void resetPassword_invalidToken_throwsException() {
        ResetPasswordRequest request = new ResetPasswordRequest();
        request.setToken("expired-token");
        request.setNewPassword("NewPass123!");

        when(tokenService.getResetTokenEmail("expired-token")).thenReturn(null);

        assertThatThrownBy(() -> authService.resetPassword(request))
                .isInstanceOf(InvalidTokenException.class);

        verify(userRepository, never()).save(any());
    }

    // --- helpers -------------------------------------------------------------

    private RegisterRequest registerRequest(String email, String password, String firstName, String lastName) {
        RegisterRequest request = new RegisterRequest();
        request.setEmail(email);
        request.setPassword(password);
        request.setFirstName(firstName);
        request.setLastName(lastName);
        return request;
    }
}
