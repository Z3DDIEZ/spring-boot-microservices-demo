package com.meridian.apigateway.config;

import org.springframework.cloud.gateway.filter.ratelimit.KeyResolver;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration class for defining rate limiting strategies within the API
 * Gateway.
 * Uses Spring Cloud Gateway's built-in Redis Rate Limiter mechanism.
 */
@Configuration
public class RateLimitConfig {

    /**
     * Defines the key used contextually by the Redis Rate Limiter to track quotas.
     * <p>
     * This implementation resolves the rate-limiting key based on the client's IP
     * address.
     * In a production environment with authenticated users, this could be adapted
     * to extract
     * a User ID or Tenant ID from the JWT token via the
     * {@link java.security.Principal}.
     *
     * @return A {@link KeyResolver} that emits the client's IP address as a string.
     */
    @Bean
    public KeyResolver remoteAddressKeyResolver() {
        return exchange -> exchange.getPrincipal()
                .map(java.security.Principal::getName)
                .defaultIfEmpty("anonymous");
    }
}
