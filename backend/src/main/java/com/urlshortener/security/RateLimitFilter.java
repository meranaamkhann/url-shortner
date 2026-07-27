package com.urlshortener.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.urlshortener.dto.response.ApiError;
import com.urlshortener.service.RateLimiterService;
import com.urlshortener.util.IpAddressUtil;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class RateLimitFilter extends OncePerRequestFilter {

    private final RateLimiterService rateLimiterService;
    private final ObjectMapper objectMapper;

    @Value("${app.rate-limit.anonymous-requests-per-minute}")
    private long anonymousPerMinute;

    @Value("${app.rate-limit.authenticated-requests-per-minute}")
    private long authenticatedPerMinute;

    @Value("${app.rate-limit.shorten-requests-per-hour}")
    private long shortenPerHour;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {

        // Health checks, metrics scraping, and docs are exempt — they're internal/infra traffic.
        String path = request.getRequestURI();
         // Only rate-limit API endpoints.
         if (!path.startsWith("/api")) {
            chain.doFilter(request, response);
            return;
        }

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        boolean authenticated = auth != null && auth.isAuthenticated()
                && auth.getPrincipal() instanceof AuthenticatedPrincipal;

        String identity = authenticated
                ? "user:" + ((AuthenticatedPrincipal) auth.getPrincipal()).id()
                : "ip:" + IpAddressUtil.resolveClientIp(request);

        long generalLimit = authenticated ? authenticatedPerMinute : anonymousPerMinute;
        boolean generalAllowed = rateLimiterService.isAllowed("ratelimit:general:" + identity, generalLimit, 60);
        if (!generalAllowed) {
            reject(response, request, "General request rate limit exceeded. Please slow down.");
            return;
        }

        boolean isCreateUrl = "POST".equalsIgnoreCase(request.getMethod())
                && (path.equals("/api/v1/urls") || path.equals("/api/v1/urls/bulk"));
        if (isCreateUrl) {
            boolean shortenAllowed = rateLimiterService.isAllowed("ratelimit:shorten:" + identity, shortenPerHour, 3600);
            if (!shortenAllowed) {
                reject(response, request, "URL-creation rate limit exceeded for this hour. Please try again later.");
                return;
            }
        }

        chain.doFilter(request, response);
    }

    private void reject(HttpServletResponse response, HttpServletRequest request, String message) throws IOException {
        response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        ApiError error = ApiError.of(HttpStatus.TOO_MANY_REQUESTS.value(), "RATE_LIMIT_EXCEEDED",
                message, request.getRequestURI(), "n/a");
        response.getWriter().write(objectMapper.writeValueAsString(error));
    }
}