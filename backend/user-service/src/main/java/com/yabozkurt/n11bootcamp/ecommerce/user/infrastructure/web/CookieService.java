package com.yabozkurt.n11bootcamp.ecommerce.user.infrastructure.web;

import com.yabozkurt.n11bootcamp.ecommerce.user.infrastructure.security.jwt.JwtService;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Service
public class CookieService {

    private final JwtService jwtService;
    private final boolean secureCookies;
    private final String sameSite;

    public CookieService(JwtService jwtService,
                         @Value("${app.security.cookies.secure:false}") boolean secureCookies,
                         @Value("${app.security.cookies.same-site:Lax}") String sameSite) {
        this.jwtService = jwtService;
        this.secureCookies = secureCookies;
        this.sameSite = sameSite;
    }

    public void setAuthCookies(HttpServletResponse response, String accessToken, String refreshToken) {
        response.addHeader(HttpHeaders.SET_COOKIE, accessCookie(accessToken, Duration.ofMillis(jwtService.getExpirationMs())).toString());
        response.addHeader(HttpHeaders.SET_COOKIE, refreshCookie(refreshToken, Duration.ofDays(jwtService.getRefreshExpirationDays())).toString());
    }

    public void clearAuthCookies(HttpServletResponse response) {
        response.addHeader(HttpHeaders.SET_COOKIE, accessCookie("", Duration.ZERO).toString());
        response.addHeader(HttpHeaders.SET_COOKIE, refreshCookie("", Duration.ZERO).toString());
    }

    private ResponseCookie accessCookie(String value, Duration maxAge) {
        return ResponseCookie.from("access_token", value)
                .httpOnly(true)
                .secure(secureCookies)
                .path("/")
                .maxAge(maxAge)
                .sameSite(sameSite)
                .build();
    }

    private ResponseCookie refreshCookie(String value, Duration maxAge) {
        return ResponseCookie.from("refresh_token", value)
                .httpOnly(true)
                .secure(secureCookies)
                .path("/")
                .maxAge(maxAge)
                .sameSite(sameSite)
                .build();
    }
}
