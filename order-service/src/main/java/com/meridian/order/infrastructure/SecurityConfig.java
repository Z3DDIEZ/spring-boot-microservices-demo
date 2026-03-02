package com.meridian.order.infrastructure;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Configures the Order Service as a stateless OAuth2 Resource Server.
 * All /api/** routes require a valid JWT; actuator health endpoints are open.
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    /**
     * Constructs the primary {@link SecurityFilterChain} for the microservice.
     * <ul>
     * <li>Globally disables CSRF protection (acceptable for stateless APIs).</li>
     * <li>Enforces session management as purely
     * {@link SessionCreationPolicy#STATELESS}.</li>
     * <li>Intercepts all routes starting with {@code /api/**} and enforces valid
     * OAuth2 Bearer Tokens.</li>
     * <li>Leaves Actuator endpoints open for infrastructure probes
     * (Kubernetes/Docker).</li>
     * </ul>
     *
     * @param http The HTTP Security builder context.
     * @return The finalized filter chain.
     * @throws Exception If an error occurs compiling the configuration block.
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/actuator/health", "/actuator/info").permitAll()
                        .requestMatchers("/api/**").authenticated()
                        .anyRequest().permitAll())
                .oauth2ResourceServer(oauth2 -> oauth2.jwt(jwt -> {
                }));

        return http.build();
    }
}
