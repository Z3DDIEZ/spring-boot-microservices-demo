package com.meridian.auth.infrastructure;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * Global Web Security Configuration for the Authentication Service.
 * <p>
 * This configuration dictates exactly which endpoints are public (registration,
 * login, refresh)
 * and which require an active session/token. It configures the application as
 * stateless,
 * relying entirely on the {@link JwtAuthenticationFilter} rather than HTTP
 * sessions.
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    public SecurityConfig(JwtAuthenticationFilter jwtAuthenticationFilter) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
    }

    /**
     * Configures the paramount {@link SecurityFilterChain}.
     * <ul>
     * <li>Disables CSRF (safe for stateless JWTs).</li>
     * <li>Permits all traffic to {@code /api/v1/auth/**} and actuator
     * endpoints.</li>
     * <li>Enforces authentication for any other endpoint.</li>
     * <li>Forces Spring to never create an HttpSession (Stateless).</li>
     * <li>Injects the JWT validation filter before Spring's default
     * username/password filter.</li>
     * </ul>
     *
     * @param http The {@link HttpSecurity} object to build configurations upon.
     * @return The fully configured Spring Security filter chain.
     * @throws Exception If an error occurs during configuration building.
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests(authz -> authz
                        .requestMatchers("/api/v1/auth/register", "/api/v1/auth/login", "/api/v1/auth/refresh")
                        .permitAll()
                        .requestMatchers("/actuator/**").permitAll()
                        .anyRequest().authenticated())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    /**
     * Exposes the {@link AuthenticationManager} as a Spring Bean so it can be
     * utilized
     * by the {@link com.meridian.auth.application.AuthService} to programmatically
     * validate credentials.
     *
     * @param authenticationConfiguration The injected configuration object.
     * @return The configured authentication manager.
     * @throws Exception If the manager cannot be retrieved.
     */
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authenticationConfiguration)
            throws Exception {
        return authenticationConfiguration.getAuthenticationManager();
    }

    /**
     * Defines the cryptographic password hashing algorithm utilized globally by the
     * service.
     * BCrypt is the industry standard for secure, salted password hashing.
     *
     * @return A {@link BCryptPasswordEncoder} instance.
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
