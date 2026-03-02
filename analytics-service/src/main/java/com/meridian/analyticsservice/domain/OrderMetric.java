package com.meridian.analyticsservice.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Pure domain POJO representing an isolated financial metric data point at a specific instant.
 * Detached entirely from framework-specific routing or persistence annotations to adhere
 * strictly to Clean Architecture bounds.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderMetric {

    private UUID orderId;
    private UUID userId;
    private BigDecimal totalAmount;
    private int itemCount;
    private Instant timestamp;
}
