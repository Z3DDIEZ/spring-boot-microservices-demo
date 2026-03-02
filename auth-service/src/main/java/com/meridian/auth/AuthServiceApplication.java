package com.meridian.auth;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * The main entry point for the Meridian Authentication Service.
 * <p>
 * This microservice oversees user registration, identity verification,
 * credential management (BCrypt hashing), and token issuance (Access &amp;
 * Refresh
 * JWTs)
 * acting as an OAuth2 Authorization Server.
 */
@SpringBootApplication
public class AuthServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(AuthServiceApplication.class, args);
    }

}
