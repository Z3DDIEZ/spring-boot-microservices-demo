package com.meridian.apigateway.config;

import org.springframework.cloud.gateway.filter.ratelimit.KeyResolver;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import reactor.core.publisher.Mono;

@Configuration
public class RateLimitConfig {

    /**
     * Resolves the key to use for rate limiting. 
     * Here we resolve by the client's IP address.
     * Alternatively, this could extract a user ID from the Principal.
     */
    @Bean
    public KeyResolver remoteAddressKeyResolver() {
        return exchange -> Mono.just(
                exchange.getRequest().getRemoteAddress() != null ? 
                exchange.getRequest().getRemoteAddress().getAddress().getHostAddress() : "unknown"
        );
    }
}
