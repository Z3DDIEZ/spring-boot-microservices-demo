package com.meridian.apigateway.config;

import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.UUID;

/**
 * Filter to ensure an X-Correlation-ID is present in all requests traversing
 * the gateway.
 * Crucial for distributed tracing.
 */
@Component
public class CorrelationIdFilter implements GlobalFilter, Ordered {

    public static final String CORRELATION_ID_HEADER_NAME = "X-Correlation-ID";

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        HttpHeaders headers = request.getHeaders();

        if (hasCorrelationId(headers)) {
            return chain.filter(exchange);
        } else {
            String correlationId = generateCorrelationId();
            ServerHttpRequest mutatedRequest = exchange.getRequest().mutate()
                    .header(CORRELATION_ID_HEADER_NAME, correlationId)
                    .build();
            return chain.filter(exchange.mutate().request(mutatedRequest).build());
        }
    }

    private boolean hasCorrelationId(HttpHeaders headers) {
        return headers.containsKey(CORRELATION_ID_HEADER_NAME);
    }

    private String generateCorrelationId() {
        return UUID.randomUUID().toString();
    }

    @Override
    public int getOrder() {
        // Execute early in the filter chain
        return -1;
    }
}
