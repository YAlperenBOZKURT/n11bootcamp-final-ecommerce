package com.yabozkurt.n11bootcamp.ecommerce.user.application.service.impl;

import com.yabozkurt.n11bootcamp.ecommerce.user.application.dto.AuthResult;
import com.yabozkurt.n11bootcamp.ecommerce.user.application.service.AuthService;
import com.yabozkurt.n11bootcamp.ecommerce.user.application.service.TokenService;
import com.yabozkurt.n11bootcamp.ecommerce.user.domain.exception.InvalidTokenException;
import com.yabozkurt.n11bootcamp.ecommerce.user.domain.exception.PhoneNumberAlreadyExistsException;
import com.yabozkurt.n11bootcamp.ecommerce.user.domain.exception.UserAlreadyExistsException;
import com.yabozkurt.n11bootcamp.ecommerce.user.domain.exception.UserNotFoundException;
import com.yabozkurt.n11bootcamp.ecommerce.user.domain.model.User;
import com.yabozkurt.n11bootcamp.ecommerce.user.domain.model.enums.Role;
import com.yabozkurt.n11bootcamp.ecommerce.user.domain.model.enums.UserStatus;
import com.yabozkurt.n11bootcamp.ecommerce.user.domain.repository.UserRepository;
import com.yabozkurt.n11bootcamp.ecommerce.user.infrastructure.security.jwt.JwtService;
import com.yabozkurt.n11bootcamp.ecommerce.user.infrastructure.security.userdetails.CustomUserDetails;
import com.yabozkurt.n11bootcamp.ecommerce.user.infrastructure.messaging.event.UserRegisteredEvent;
import com.yabozkurt.n11bootcamp.ecommerce.user.infrastructure.messaging.event.PasswordResetRequestedEvent;
import com.yabozkurt.n11bootcamp.ecommerce.user.infrastructure.messaging.publisher.UserEventPublisher;
import com.yabozkurt.n11bootcamp.ecommerce.user.presentation.dto.request.ForgotPasswordRequest;
import com.yabozkurt.n11bootcamp.ecommerce.user.presentation.dto.request.LoginRequest;
import com.yabozkurt.n11bootcamp.ecommerce.user.presentation.dto.request.RegisterRequest;
import com.yabozkurt.n11bootcamp.ecommerce.user.presentation.dto.request.ResetPasswordRequest;
import io.jsonwebtoken.JwtException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class AuthServiceImpl implements AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthServiceImpl.class);

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final TokenService tokenService;
    private final AuthenticationManager authenticationManager;
    private final UserEventPublisher eventPublisher;

    public AuthServiceImpl(UserRepository userRepository,
                           PasswordEncoder passwordEncoder,
                           JwtService jwtService,
                           TokenService tokenService,
                           AuthenticationManager authenticationManager,
                           UserEventPublisher eventPublisher) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.tokenService = tokenService;
        this.authenticationManager = authenticationManager;
        this.eventPublisher = eventPublisher;
    }

    @Override
    @Transactional
    public AuthResult register(RegisterRequest request) {
        log.info("Register attempt: {}", request.getEmail());
        String normalizedPhone = normalizePhone(request.getPhoneNumber());

        User existingUser = userRepository.findByEmail(request.getEmail()).orElse(null);
        if (existingUser != null) {
            if (existingUser.getStatus() == UserStatus.DELETED) {
                ensurePhoneNumberUniqueForRegister(normalizedPhone, existingUser.getId());
                existingUser.setPassword(passwordEncoder.encode(request.getPassword()));
                existingUser.setFirstName(request.getFirstName());
                existingUser.setLastName(request.getLastName());
                existingUser.setPhoneNumber(normalizedPhone);
                existingUser.setRole(Role.CUSTOMER);
                existingUser.setStatus(UserStatus.ACTIVE);
                userRepository.save(existingUser);
                log.info("Deleted user reactivated via register: {}", existingUser.getEmail());
                return issueTokens(existingUser);
            }

            log.warn("Register failed - email already exists: {}", request.getEmail());
            throw new UserAlreadyExistsException(request.getEmail());
        }
        ensurePhoneNumberUniqueForRegister(normalizedPhone, null);

        User user = new User(
                request.getEmail(),
                passwordEncoder.encode(request.getPassword()),
                request.getFirstName(),
                request.getLastName(),
                normalizedPhone,
                Role.CUSTOMER
        );

        userRepository.save(user);
        log.info("User registered successfully: {}", user.getEmail());
        eventPublisher.publishUserRegistered(
                new UserRegisteredEvent(user.getId(), user.getEmail(), user.getFirstName(), user.getLastName()));
        return issueTokens(user);
    }

    @Override
    @Transactional
    public AuthResult login(LoginRequest request) {
        log.info("Login attempt: {}", request.getEmail());

        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword())
        );

        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new UserNotFoundException(request.getEmail()));
        ensureUserCanAuthenticate(user);

        log.info("Login successful: {}", user.getEmail());
        return issueTokens(user);
    }

    @Override
    @Transactional
    public AuthResult refresh(String refreshToken) {
        if (tokenService.isTokenBlacklisted(refreshToken)) {
            log.warn("Refresh token is blacklisted");
            throw new InvalidTokenException("Refresh token is invalid or expired");
        }

        String email;
        try {
            if (!jwtService.isRefreshToken(refreshToken)) {
                log.warn("Invalid token type used for refresh");
                throw new InvalidTokenException("Invalid token type");
            }
            email = jwtService.extractEmail(refreshToken);
        } catch (JwtException | IllegalArgumentException e) {
            // We convert JWT errors to InvalidTokenException here to keep 401 responses stable.
            log.warn("Invalid refresh token format or signature");
            throw new InvalidTokenException("Refresh token is invalid or expired");
        }

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UserNotFoundException(email));
        ensureUserCanAuthenticate(user);

        long remainingTtl = jwtService.getRemainingTtlMs(refreshToken);
        if (remainingTtl > 0) {
            tokenService.blacklistToken(refreshToken, remainingTtl);
        }

        log.info("Token refreshed for: {}", email);
        return issueTokens(user);
    }

    @Override
    public void logout(String accessToken, String refreshToken) {
        blacklistIfValid(accessToken);
        blacklistIfValid(refreshToken);
        log.info("User logged out");
    }

    private void ensureUserCanAuthenticate(User user) {
        if (user.getStatus() == UserStatus.FROZEN) {
            throw new InvalidTokenException("Account is frozen");
        }
        if (user.getStatus() == UserStatus.DELETED) {
            throw new InvalidTokenException("Account is deleted");
        }
    }

    private void blacklistIfValid(String token) {
        if (token == null) return;
        try {
            long remainingTtl = jwtService.getRemainingTtlMs(token);
            if (remainingTtl > 0) {
                tokenService.blacklistToken(token, remainingTtl);
            }
        } catch (Exception e) {
            log.warn("Could not blacklist token: {}", e.getMessage());
        }
    }

    private static final long RESET_TOKEN_TTL_MS = 15 * 60 * 1000L; // 15 minutes

    @Override
    @Transactional
    public void forgotPassword(ForgotPasswordRequest request) {
        // Always return success to avoid user enumeration for security and  publish event if email exists and account is active
        userRepository.findByEmail(request.getEmail()).ifPresent(user -> {
            if (user.getStatus() == UserStatus.ACTIVE) {
                String token = UUID.randomUUID().toString();
                tokenService.storeResetToken(token, user.getEmail(), RESET_TOKEN_TTL_MS);
                eventPublisher.publishPasswordResetRequested(new PasswordResetRequestedEvent(
                        user.getId(),
                        user.getEmail(),
                        user.getFirstName(),
                        token,
                        RESET_TOKEN_TTL_MS / 1000
                ));
                log.info("Password reset requested event published for {}", user.getEmail());
            }
        });
    }

    @Override
    @Transactional
    public void resetPassword(ResetPasswordRequest request) {
        String email = tokenService.getResetTokenEmail(request.getToken());
        if (email == null) {
            throw new InvalidTokenException("Password reset link is invalid or expired");
        }

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UserNotFoundException(email));

        if (user.getStatus() != UserStatus.ACTIVE) {
            throw new InvalidTokenException("Account is not active");
        }

        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);
        tokenService.deleteResetToken(request.getToken());
        log.info("Password reset successful for: {}", email);
    }

    private AuthResult issueTokens(User user) {
        CustomUserDetails userDetails = new CustomUserDetails(user);
        String accessToken = jwtService.generateAccessToken(userDetails);
        String refreshToken = jwtService.generateRefreshToken(userDetails);

        return new AuthResult(
                accessToken,
                refreshToken,
                user.getEmail(),
                user.getFirstName(),
                user.getLastName(),
                user.getRole().name()
        );
    }

    private void ensurePhoneNumberUniqueForRegister(String phoneNumber, Long currentUserId) {
        if (phoneNumber == null) return;
        boolean exists = currentUserId == null
                ? userRepository.existsByPhoneNumberAndStatusNot(phoneNumber, UserStatus.DELETED)
                : userRepository.existsByPhoneNumberAndIdNotAndStatusNot(phoneNumber, currentUserId, UserStatus.DELETED);
        if (exists) {
            throw new PhoneNumberAlreadyExistsException(phoneNumber);
        }
    }

    private String normalizePhone(String phoneNumber) {
        if (phoneNumber == null) return null;
        String trimmed = phoneNumber.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
