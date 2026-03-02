package com.meridian.apigateway;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * The main entry point for the Meridian API Gateway based on Spring Cloud
 * Gateway.
 * <p>
 * The API Gateway is the central reverse proxy and entry point for all external
 * client
 * requests. It handles dynamic routing, rate limiting (Redis), circuit breaking
 * (Resilience4j),
 * and centralized OAuth2/JWT token validation before forwarding requests to
 * downstream microservices.
 */
@SpringBootApplication
public class ApiGatewayApplication {

    /**
     * Bootstrap the Spring Boot application.
     *
     * @param args Command line arguments passed to the application.
     */
    public static void main(String[] args) {
        SpringApplication.run(ApiGatewayApplication.class, args);
    }
}
