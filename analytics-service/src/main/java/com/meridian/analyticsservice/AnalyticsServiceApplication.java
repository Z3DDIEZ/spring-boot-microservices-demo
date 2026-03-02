package com.meridian.analyticsservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Meridian Analytics Service Application Entry Point.
 * <p>
 * This microservice silently taps into the distributed AMQP event stream to aggregate 
 * historical timeseries data without degrading the operational paths of core services.
 */
@SpringBootApplication
public class AnalyticsServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(AnalyticsServiceApplication.class, args);
    }
}
