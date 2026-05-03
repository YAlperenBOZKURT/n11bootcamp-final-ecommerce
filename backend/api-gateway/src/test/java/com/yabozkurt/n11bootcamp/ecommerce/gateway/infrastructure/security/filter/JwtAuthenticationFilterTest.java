package com.yabozkurt.n11bootcamp.ecommerce.gateway.infrastructure.security.filter;

import com.yabozkurt.n11bootcamp.ecommerce.gateway.infrastructure.security.config.GatewaySecurityProperties;
import com.yabozkurt.n11bootcamp.ecommerce.gateway.infrastructure.security.jwt.JwtService;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpMethod;
import org.springframework.test.util.ReflectionTestUtils;

import javax.crypto.SecretKey;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class JwtAuthenticationFilterTest {

    private static final String SECRET = "ecommerce-secret-key-n11bootcamp-yabozkurt-2025";

    private JwtService jwtService;
    private JwtAuthenticationFilter filter;

    @Mock HttpServletRequest request;
    @Mock HttpServletResponse response;
    @Mock FilterChain filterChain;

    @BeforeEach
    void setUp() {
        jwtService = new JwtService();
        ReflectionTestUtils.setField(jwtService, "secretKey", SECRET);

        GatewaySecurityProperties props = new GatewaySecurityProperties();
        props.setPublicEndpoints(List.of("/api/users/auth/**", "/actuator/**"));

        filter = new JwtAuthenticationFilter(jwtService, props);
    }

    // -- shouldNotFilter -------------------------------------------------------

    @Test
    void shouldNotFilter_publicEndpoint_returnsTrue() {
        when(request.getMethod()).thenReturn(HttpMethod.GET.name());
        when(request.getRequestURI()).thenReturn("/api/users/auth/login");
        assertThat(filter.shouldNotFilter(request)).isTrue();
    }

    @Test
    void shouldNotFilter_optionsMethod_returnsTrue() {
        when(request.getMethod()).thenReturn(HttpMethod.OPTIONS.name());
        assertThat(filter.shouldNotFilter(request)).isTrue();
    }

    @Test
    void shouldNotFilter_protectedEndpoint_returnsFalse() {
        when(request.getMethod()).thenReturn(HttpMethod.GET.name());
        when(request.getRequestURI()).thenReturn("/api/products/1");
        assertThat(filter.shouldNotFilter(request)).isFalse();
    }

    // -- doFilterInternal ------------------------------------------------------

    @Test
    void doFilterInternal_noToken_returns401() throws Exception {
        when(request.getHeader("Authorization")).thenReturn(null);
        when(request.getCookies()).thenReturn(null);
        when(response.getWriter()).thenReturn(new PrintWriter(new StringWriter()));

        filter.doFilterInternal(request, response, filterChain);

        verify(response).setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        verify(filterChain, never()).doFilter(any(), any());
    }

    @Test
    void doFilterInternal_refreshToken_returns401() throws Exception {
        String refreshToken = buildToken("user@test.com", "refresh", null, null, 60_000);
        when(request.getHeader("Authorization")).thenReturn("Bearer " + refreshToken);
        when(response.getWriter()).thenReturn(new PrintWriter(new StringWriter()));

        filter.doFilterInternal(request, response, filterChain);

        verify(response).setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        verify(filterChain, never()).doFilter(any(), any());
    }

    @Test
    void doFilterInternal_invalidToken_returns401() throws Exception {
        when(request.getHeader("Authorization")).thenReturn("Bearer not.a.token");
        when(response.getWriter()).thenReturn(new PrintWriter(new StringWriter()));

        filter.doFilterInternal(request, response, filterChain);

        verify(response).setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        verify(filterChain, never()).doFilter(any(), any());
    }

    @Test
    void doFilterInternal_expiredToken_returns401() throws Exception {
        String expired = buildToken("user@test.com", "access", "ROLE_CUSTOMER", 1L, -1000);
        when(request.getHeader("Authorization")).thenReturn("Bearer " + expired);
        when(response.getWriter()).thenReturn(new PrintWriter(new StringWriter()));

        filter.doFilterInternal(request, response, filterChain);

        verify(response).setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        verify(filterChain, never()).doFilter(any(), any());
    }

    @Test
    void doFilterInternal_validTokenFromHeader_proceedsAndAddsUserHeaders() throws Exception {
        String token = buildToken("ali@test.com", "access", "ROLE_CUSTOMER", 7L, 60_000);
        when(request.getHeader("Authorization")).thenReturn("Bearer " + token);

        ArgumentCaptor<HttpServletRequest> reqCaptor = ArgumentCaptor.forClass(HttpServletRequest.class);

        filter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(reqCaptor.capture(), eq(response));
        HttpServletRequest enriched = reqCaptor.getValue();
        assertThat(enriched.getHeader("X-User-Email")).isEqualTo("ali@test.com");
        assertThat(enriched.getHeader("X-User-Role")).isEqualTo("ROLE_CUSTOMER");
        assertThat(enriched.getHeader("X-User-Id")).isEqualTo("7");
    }

    @Test
    void doFilterInternal_validTokenFromCookie_proceedsAndAddsUserHeaders() throws Exception {
        String token = buildToken("veli@test.com", "access", "ROLE_ADMIN", 3L, 60_000);
        Cookie accessCookie = new Cookie("access_token", token);
        when(request.getHeader("Authorization")).thenReturn(null);
        when(request.getCookies()).thenReturn(new Cookie[]{accessCookie});

        ArgumentCaptor<HttpServletRequest> reqCaptor = ArgumentCaptor.forClass(HttpServletRequest.class);

        filter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(reqCaptor.capture(), eq(response));
        assertThat(reqCaptor.getValue().getHeader("X-User-Email")).isEqualTo("veli@test.com");
        assertThat(reqCaptor.getValue().getHeader("X-User-Role")).isEqualTo("ROLE_ADMIN");
    }

    @Test
    void doFilterInternal_headerTakesPriorityOverCookie() throws Exception {
        String headerToken = buildToken("header@test.com", "access", "ROLE_CUSTOMER", 1L, 60_000);
        when(request.getHeader("Authorization")).thenReturn("Bearer " + headerToken);
        // No cookie stub — header is found first, getCookies() is never called

        ArgumentCaptor<HttpServletRequest> reqCaptor = ArgumentCaptor.forClass(HttpServletRequest.class);

        filter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(reqCaptor.capture(), eq(response));
        assertThat(reqCaptor.getValue().getHeader("X-User-Email")).isEqualTo("header@test.com");
    }

    // -- helper ----------------------------------------------------------------

    private String buildToken(String subject, String type, String role, Long userId, long expiryMs) {
        SecretKey key = Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8));
        var builder = Jwts.builder()
                .subject(subject)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + expiryMs))
                .claim("type", type);
        if (role != null) builder.claim("role", role);
        if (userId != null) builder.claim("userId", userId);
        return builder.signWith(key).compact();
    }
}
