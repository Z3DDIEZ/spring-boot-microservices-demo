package com.meridian.order.presentation.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Outbound DTO detailing an inherent, finalized line item encapsulated within
 * an Order.
 * Purposely strips out bidirectional parent references to avoid cyclic
 * serialization bombs.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderItemResponse {

    private UUID id;
    private UUID productId;
    private Integer quantity;
    private BigDecimal unitPrice;
}
