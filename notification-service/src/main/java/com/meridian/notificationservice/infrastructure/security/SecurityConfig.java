package com.meridian.notificationservice.infrastructure.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;

import org.springframework.security.web.SecurityFilterChain;

/**
 * Locks down the microservice presentation perimeter.
 * Because Notification Service strictly operates off internal message queues,
 * practically all native HTTP ingress paths (aside from health actuators) are
 * permanently denied.
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        // Allow actuator health checks
                        .requestMatchers("/actuator/health", "/actuator/prometheus").permitAll()
                        // Block literally everything else (this service is purely async/headless)
                        .anyRequest().denyAll());

        return http.build();
    }
}
