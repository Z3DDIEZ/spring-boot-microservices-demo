package com.meridian.inventoryservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Meridian Inventory Service Application Entry Point.
 * <p>
 * This microservice manages the catalog's physical state using MongoDB. It is
 * highly reactive,
 * listening to architectural events spanning from the distributed broker to
 * reliably
 * lock or release product stock without requiring synchronously coupled HTTP
 * requests.
 */
@SpringBootApplication
public class InventoryServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(InventoryServiceApplication.class, args);
    }
}
