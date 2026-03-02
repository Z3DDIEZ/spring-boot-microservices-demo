package com.meridian.order.application;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Bootstraps Spring's background task execution capabilities.
 * Required for the {@link OutboxScheduler} to periodically poll the database.
 */
@Configuration
@EnableScheduling
public class SchedulingConfig {
}
