package com.meridian.order;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * The main entry point for the Meridian Order Management Service.
 * <p>
 * Orchestrates order placement, leverages Spring Data JPA (PostgreSQL) for
 * transactional boundaries,
 * and heavily utilizes the Transactional Outbox pattern synchronized with
 * RabbitMQ to maintain
 * saga consistency across the wider distributed platform.
 */
@SpringBootApplication
public class OrderServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(OrderServiceApplication.class, args);
    }

}
