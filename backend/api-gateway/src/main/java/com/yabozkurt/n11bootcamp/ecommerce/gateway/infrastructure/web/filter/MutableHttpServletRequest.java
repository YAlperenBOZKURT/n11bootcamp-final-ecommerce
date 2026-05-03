package com.yabozkurt.n11bootcamp.ecommerce.gateway.infrastructure.web.filter;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;

import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Vector;

public class MutableHttpServletRequest extends HttpServletRequestWrapper {

    private final Map<String, String> customHeaders = new HashMap<>();

    public MutableHttpServletRequest(HttpServletRequest request) {
        super(request);
    }

    public void putHeader(String name, String value) {
        customHeaders.put(name, value);
    }

    @Override
    public String getHeader(String name) {
        String headerValue = customHeaders.get(name);
        if (headerValue != null) {
            return headerValue;
        }
        return ((HttpServletRequest) getRequest()).getHeader(name);
    }

    @Override
    public Enumeration<String> getHeaders(String name) {
        if (customHeaders.containsKey(name)) {
            return Collections.enumeration(Collections.singletonList(customHeaders.get(name)));
        }
        return ((HttpServletRequest) getRequest()).getHeaders(name);
    }

    @Override
    public Enumeration<String> getHeaderNames() {
        List<String> headerNames = Collections.list(((HttpServletRequest) getRequest()).getHeaderNames());
        for (String customHeaderName : customHeaders.keySet()) {
            if (!headerNames.contains(customHeaderName)) {
                headerNames.add(customHeaderName);
            }
        }
        return new Vector<>(headerNames).elements();
    }
}
