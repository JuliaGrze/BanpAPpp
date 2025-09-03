package com.bank.bank.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class ApiKeyFilter extends OncePerRequestFilter {

    private final String expectedKey;

    public ApiKeyFilter(@Value("${bank.api-key}") String expectedKey) {
        this.expectedKey = expectedKey;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {

        // Sprawdzamy tylko endpoint inicjowania transakcji
        if (request.getRequestURI().equals("/api/transactions/init")) {
            String header = request.getHeader("X-API-KEY");
            if (header == null || !header.equals(expectedKey)) {
                response.sendError(HttpServletResponse.SC_FORBIDDEN, "Invalid API key");
                return;
            }
        }
        filterChain.doFilter(request, response);
    }
}
