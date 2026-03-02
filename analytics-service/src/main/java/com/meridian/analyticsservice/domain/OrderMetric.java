package com.meridian.analyticsservice.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Pure domain POJO representing an order metric data point.
 * No framework annotations -- domain remains clean.
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
