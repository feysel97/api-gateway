package com.api.api_gateway.filter;

import com.api.api_gateway.security.JwtUtils;
import io.jsonwebtoken.Claims;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.List;

@Component
public class JwtAuthenticationFilter implements GlobalFilter, Ordered {

    private static final Logger log = LoggerFactory.getLogger(JwtAuthenticationFilter.class);

    @Autowired
    private JwtUtils jwtUtils;

    private static final List<String> PUBLIC_ENDPOINTS = List.of(
            "/api/auth"
    );

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        String path = request.getURI().getPath();

        // 1. Bypass public auth routes
        if (isPublicEndpoint(path)) {
            return chain.filter(exchange);
        }

        // 2. Validate Authorization header presence
        if (!request.getHeaders().containsKey(HttpHeaders.AUTHORIZATION)) {
            log.warn("Missing Authorization header for path: {}", path);
            return onError(exchange, HttpStatus.UNAUTHORIZED);
        }

        String authHeader = request.getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            log.warn("Invalid Authorization header format for path: {}", path);
            return onError(exchange, HttpStatus.UNAUTHORIZED);
        }

        String token = authHeader.substring(7);

        // 3. Validate JWT signature and expiration
        if (!jwtUtils.isTokenValid(token)) {
            log.warn("Token validation failed for path: {}", path);
            return onError(exchange, HttpStatus.UNAUTHORIZED);
        }

        // 4. Extract claims and forward headers downstream
        Claims claims = jwtUtils.extractAllClaims(token);
        String username = claims.getSubject();
        String userEmail = claims.get("email", String.class);

        Object userIdObj = claims.get("userId");
        String userId = userIdObj != null ? String.valueOf(userIdObj) : "";

        // Extract roles (JwtService sets it as a List<String>)
        List<?> roles = claims.get("roles", List.class);
        String rolesString = roles != null ? String.join(",", roles.stream().map(Object::toString).toList()) : "";

        ServerHttpRequest mutatedRequest = request.mutate()
                .header("X-User-Email", userEmail != null ? userEmail : "")
                .header("X-User-Id", userId)
                .header("X-User-Roles", rolesString) // Inject roles header
                .build();

        return chain.filter(exchange.mutate().request(mutatedRequest).build());
    }

    private boolean isPublicEndpoint(String path) {
        return PUBLIC_ENDPOINTS.stream().anyMatch(path::startsWith);
    }

    private Mono<Void> onError(ServerWebExchange exchange, HttpStatus httpStatus) {
        exchange.getResponse().setStatusCode(httpStatus);
        return exchange.getResponse().setComplete();
    }

    @Override
    public int getOrder() {
        return 0; // Runs right after LoggingAndTrackingFilter (-1)
    }
}