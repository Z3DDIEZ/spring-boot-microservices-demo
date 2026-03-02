package com.meridian.analyticsservice.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

/**
 * Pure domain POJO representing an inventory reservation metric data point.
 * No framework annotations -- domain remains clean.
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
