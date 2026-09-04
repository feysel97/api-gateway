package com.api.api_gateway.filter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.UUID;

@Component
public class LoggingAndTrackingFilter implements GlobalFilter, Ordered {

    private static final Logger log = LoggerFactory.getLogger(LoggingAndTrackingFilter.class);
    public static final String CORRELATION_ID_HEADER = "X-Request-ID";

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();

        // 1. Extract existing X-Request-ID or generate a new UUID (effectively final)
        String incomingId = request.getHeaders().getFirst(CORRELATION_ID_HEADER);
        final String correlationId = (incomingId != null && !incomingId.isBlank())
                ? incomingId
                : UUID.randomUUID().toString();

        // 2. Attach Correlation ID to the downstream request header
        ServerHttpRequest mutatedRequest = request.mutate()
                .header(CORRELATION_ID_HEADER, correlationId)
                .build();

        ServerWebExchange mutatedExchange = exchange.mutate().request(mutatedRequest).build();

        long startTime = System.currentTimeMillis();
        log.info("[Gateway IN] ID: {} | Method: {} | Path: {}",
                correlationId, request.getMethod(), request.getURI().getPath());

        // 3. Process request and log response timing
        return chain.filter(mutatedExchange).then(Mono.fromRunnable(() -> {
            long duration = System.currentTimeMillis() - startTime;
            log.info("[Gateway OUT] ID: {} | Status: {} | Time: {}ms",
                    correlationId, exchange.getResponse().getStatusCode(), duration);
        }));
    }

    @Override
    public int getOrder() {
        return -1; // Highest priority: execute before routing
    }
}