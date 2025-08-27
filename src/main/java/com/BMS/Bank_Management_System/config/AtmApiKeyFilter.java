package com.BMS.Bank_Management_System.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@Component
@RequiredArgsConstructor
public class AtmApiKeyFilter extends OncePerRequestFilter {

    @Value("${atm.api.key}")
    private String validApiKey;

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain)
            throws ServletException, IOException {

        String path = request.getRequestURI();
        String apiKey = request.getHeader("X-ATM-API-KEY");

        if (path.startsWith("/api/atm/") ||
                path.equals("/api/cardless/withdrawal/process") ||
                path.startsWith("/api/cardless/withdrawal/validate/")) {

            if (apiKey == null || apiKey.isBlank()) {
                response.sendError(HttpStatus.UNAUTHORIZED.value(), "ATM API Key is required");
                return;
            }

            if (!apiKey.equals(validApiKey)) {
                response.sendError(HttpStatus.UNAUTHORIZED.value(), "Invalid ATM API Key");
                return;
            }

            Authentication authentication = new UsernamePasswordAuthenticationToken(
                    "ATM-SYSTEM",
                    null,
                    List.of(new SimpleGrantedAuthority("ROLE_ATM"))
            );
            SecurityContextHolder.getContext().setAuthentication(authentication);
        }

        filterChain.doFilter(request, response);
    }
}