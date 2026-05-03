package com.yabozkurt.n11bootcamp.ecommerce.gateway.infrastructure.security.config;

import com.yabozkurt.n11bootcamp.ecommerce.gateway.infrastructure.security.filter.JwtAuthenticationFilter;
import com.yabozkurt.n11bootcamp.ecommerce.gateway.infrastructure.web.config.GatewayCorsProperties;
import com.yabozkurt.n11bootcamp.ecommerce.gateway.infrastructure.web.filter.CorrelationIdFilter;
import com.yabozkurt.n11bootcamp.ecommerce.gateway.infrastructure.web.filter.RequestLoggingFilter;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AuthorizeHttpRequestsConfigurer;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Locale;

@Configuration
@EnableConfigurationProperties({GatewaySecurityProperties.class, GatewayCorsProperties.class})
public class SecurityConfig {

    private final GatewaySecurityProperties securityProperties;
    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final CorrelationIdFilter correlationIdFilter;
    private final RequestLoggingFilter requestLoggingFilter;
    private final GatewayCorsProperties corsProperties;

    public SecurityConfig(GatewaySecurityProperties securityProperties,
                          JwtAuthenticationFilter jwtAuthenticationFilter,
                          CorrelationIdFilter correlationIdFilter,
                          RequestLoggingFilter requestLoggingFilter,
                          GatewayCorsProperties corsProperties) {
        this.securityProperties = securityProperties;
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
        this.correlationIdFilter = correlationIdFilter;
        this.requestLoggingFilter = requestLoggingFilter;
        this.corsProperties = corsProperties;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        String[] publicEndpoints    = securityProperties.getPublicEndpoints().toArray(new String[0]);
        String[] publicGetEndpoints = securityProperties.getPublicGetEndpoints().toArray(new String[0]);

        http
                .csrf(AbstractHttpConfigurer::disable)
                .cors(Customizer.withDefaults())
                .httpBasic(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> {
                    auth.requestMatchers(HttpMethod.OPTIONS, "/**").permitAll();

                    // Public endpoints (any method — auth pages, actuator, swagger)
                    auth.requestMatchers(publicEndpoints).permitAll();

                    // -- Admin endpoints (method + path based) 
                    applyAdminRules(auth);

                    // -- Public GET endpoints (after admin rules so admin paths are not opened) 
                    if (publicGetEndpoints.length > 0) {
                        auth.requestMatchers(HttpMethod.GET, publicGetEndpoints).permitAll();
                    }

                    auth.anyRequest().authenticated();
                });

        http.addFilterBefore(correlationIdFilter, UsernamePasswordAuthenticationFilter.class);
        http.addFilterAfter(requestLoggingFilter, CorrelationIdFilter.class);
        http.addFilterAfter(jwtAuthenticationFilter, RequestLoggingFilter.class);

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(corsProperties.getAllowedOrigins());
        configuration.setAllowedMethods(corsProperties.getAllowedMethods());
        configuration.setAllowedHeaders(corsProperties.getAllowedHeaders());
        configuration.setAllowCredentials(corsProperties.isAllowCredentials());
        configuration.setMaxAge(corsProperties.getMaxAgeSeconds());

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    private void applyAdminRules(AuthorizeHttpRequestsConfigurer<HttpSecurity>.AuthorizationManagerRequestMatcherRegistry auth) {
        // i did not find better solution than parsing method
        for (String rule : securityProperties.getAdminEndpoints()) {
            if (rule == null || rule.isBlank() || !rule.contains(":")) {
                continue;
            }
            String[] parts = rule.split(":", 2);
            HttpMethod method = HttpMethod.valueOf(parts[0].trim().toUpperCase(Locale.ROOT));
            String pattern = parts[1].trim();
            auth.requestMatchers(method, pattern).hasAnyAuthority("ADMIN", "ROLE_ADMIN");
        }

    }
}
