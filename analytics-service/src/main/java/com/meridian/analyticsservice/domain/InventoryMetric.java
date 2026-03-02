package com.meridian.analyticsservice.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

/**
 * Pure domain POJO representing an isolated inventory metric data point at a specific instant.
 * Detached entirely from framework-specific routing or persistence annotations to adhere
 * strictly to Clean Architecture bounds.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InventoryMetric {

    private UUID orderId;
    private UUID productId;
    private int quantity;
    private String reservationStatus;
    private Instant timestamp;
}
