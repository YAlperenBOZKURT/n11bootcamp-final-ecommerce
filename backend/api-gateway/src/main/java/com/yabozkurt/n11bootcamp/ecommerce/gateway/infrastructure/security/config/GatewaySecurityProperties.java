package com.yabozkurt.n11bootcamp.ecommerce.gateway.infrastructure.security.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.List;

@ConfigurationProperties(prefix = "gateway.security")
public class GatewaySecurityProperties {

    private List<String> publicEndpoints = new ArrayList<>();

    // Paths that are public only for GET requests (e.g. product browsing).
    private List<String> publicGetEndpoints = new ArrayList<>();
    // Method-based admin protected endpoints in METHOD:/path/** format.
    private List<String> adminEndpoints = new ArrayList<>();

    public List<String> getPublicEndpoints() {
        return publicEndpoints;
    }

    public void setPublicEndpoints(List<String> publicEndpoints) {
        this.publicEndpoints = publicEndpoints;
    }

    public List<String> getPublicGetEndpoints() {
        return publicGetEndpoints;
    }

    public void setPublicGetEndpoints(List<String> publicGetEndpoints) {
        this.publicGetEndpoints = publicGetEndpoints;
    }

    public List<String> getAdminEndpoints() {
        return adminEndpoints;
    }

    public void setAdminEndpoints(List<String> adminEndpoints) {
        this.adminEndpoints = adminEndpoints;
    }
}
