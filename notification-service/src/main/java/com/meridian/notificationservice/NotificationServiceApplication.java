package com.meridian.notificationservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Meridian Notification Service Application Entry Point.
 * <p>
 * This microservice functions as an isolated infrastructure binding layer,
 * decoupling
 * the core business logic domains from physical email or SMS transport
 * implementations.
 */
@SpringBootApplication
public class NotificationServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(NotificationServiceApplication.class, args);
    }
}
