package com.yabozkurt.n11bootcamp.ecommerce.gateway.infrastructure.web.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class RequestLoggingFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(RequestLoggingFilter.class);

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        long start = System.currentTimeMillis();

        try {
            filterChain.doFilter(request, response);
        } finally {
            long durationMs = System.currentTimeMillis() - start;
            int status = response.getStatus();
            String correlationId = MDC.get(CorrelationIdFilter.CORRELATION_ID_MDC_KEY);

            if (status >= 500) {
                log.warn("Gateway request method={} path={} status={} durationMs={} correlationId={}",
                        request.getMethod(), request.getRequestURI(), status, durationMs, correlationId);
            } else {
                log.info("Gateway request method={} path={} status={} durationMs={} correlationId={}",
                        request.getMethod(), request.getRequestURI(), status, durationMs, correlationId);
            }
        }
    }
}

