package com.meridian.inventoryservice.application.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Lightweight Data Transfer Object capturing the essence of a requested line
 * item
 * originating from the distant point-of-sale logic.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrderItemDto {
    private String sku;
    private Integer quantity;
    private BigDecimal unitPrice;
}
