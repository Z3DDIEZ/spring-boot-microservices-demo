package com.meridian.apigateway.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.oauth2.jwt.NimbusReactiveJwtDecoder;
import org.springframework.security.oauth2.jwt.ReactiveJwtDecoder;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.reactive.CorsConfigurationSource;
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource;

import javax.crypto.spec.SecretKeySpec;
import java.util.Arrays;

/**
 * Global Security Configuration for the API Gateway using Spring WebFlux
 * Security.
 * <p>
 * This configuration acts as an OAuth2 Resource Server. It intercepts all
 * incoming requests,
 * validating JWT access tokens using the shared HMAC-SHA256 secret. It enforces
 * authentication
 * on all routes except explicitly permitted public endpoints (e.g.,
 * authentication, actuator).
 */
@Configuration
@EnableWebFluxSecurity
public class SecurityConfig {

    /**
     * The symmetric secret key used to verify JWT signatures, injected from
     * application properties.
     */
    @Value("${jwt.secret}")
    private String jwtSecret;

    /**
     * Configures the core {@link SecurityWebFilterChain} for the gateway.
     * <ul>
     * <li>Disables CSRF (safe for stateless API/JWT architecture).</li>
     * <li>Configures global CORS settings.</li>
     * <li>Permits traffic to auth and actuator endpoints.</li>
     * <li>Enforces authentication on all other endpoints.</li>
     * <li>Sets up the OAuth2 Resource Server for JWT parsing.</li>
     * </ul>
     *
     * @param http The {@link ServerHttpSecurity} to build upon.
     * @return The constructed reactive security filter chain.
     */
    @Bean
    public SecurityWebFilterChain springSecurityFilterChain(ServerHttpSecurity http) {
        http
                .csrf(ServerHttpSecurity.CsrfSpec::disable)
                .cors(corsSpec -> corsSpec.configurationSource(corsConfigurationSource()))
                .authorizeExchange(exchanges -> exchanges
                        // Allow public access to auth endpoints and actuator
                        .pathMatchers("/api/v1/auth/register", "/api/v1/auth/login", "/api/v1/auth/refresh").permitAll()
                        .pathMatchers("/actuator/**").permitAll()
                        // Require auth for everything else
                        .anyExchange().authenticated())
                .oauth2ResourceServer(oauth2 -> oauth2
                        .jwt(jwt -> jwt.jwtDecoder(reactiveJwtDecoder())));

        return http.build();
    }

    /**
     * Configures the reactive JWT decoder.
     * Parses the shared secret key and prepares the {@link ReactiveJwtDecoder} to
     * validate the incoming MAC signatures on tokens transparently.
     *
     * @return The configured JWT decoder instance.
     */
    @Bean
    public ReactiveJwtDecoder reactiveJwtDecoder() {
        byte[] keyBytes = java.util.Base64.getDecoder().decode(jwtSecret);
        SecretKeySpec secretKey = new SecretKeySpec(keyBytes, "HmacSHA256");
        return NimbusReactiveJwtDecoder.withSecretKey(secretKey).build();
    }

    /**
     * Defines global Cross-Origin Resource Sharing (CORS) rules.
     * Currently configured permissively for local development.
     * In a production setting, `allowedOrigins` should be locked down to the
     * frontend domain.
     *
     * @return The configured CORS source.
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(Arrays.asList("*")); // In production, replace with specific origins
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(Arrays.asList("*"));

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}
