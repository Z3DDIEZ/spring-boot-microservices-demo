package com.meridian.inventoryservice.application.dto;

import com.meridian.inventoryservice.domain.Product;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Explicit Outbound DTO serving as a pristine presentation of a domain
 * {@link Product}.
 * Safely flattens the rich entity representation into primitive REST
 * compatibility.
 */
@Data
@Builder
public class ProductResponse {
    private String id;
    private String sku;
    private String name;
    private String description;
    private String categoryId;
    private BigDecimal price;
    private Integer stockQuantity;
    private Instant createdAt;
    private Instant updatedAt;

    public static ProductResponse fromEntity(Product product) {
        return ProductResponse.builder()
                .id(product.getId())
                .sku(product.getSku())
                .name(product.getName())
                .description(product.getDescription())
                .categoryId(product.getCategoryId())
                .price(product.getPrice())
                .stockQuantity(product.getStockQuantity())
                .createdAt(product.getCreatedAt())
                .updatedAt(product.getUpdatedAt())
                .build();
    }
}
