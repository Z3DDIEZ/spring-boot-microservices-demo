package com.meridian.order.presentation.dto;

import com.meridian.order.domain.Order;
import com.meridian.order.domain.OrderItem;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Outgoing DTO representing a complete order with its line items.
 * Domain entities are never exposed directly in REST payloads.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderResponse {

    private UUID id;
    private UUID userId;
    private String status;
    private BigDecimal totalAmount;
    private List<OrderItemResponse> items;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    /**
     * Maps a domain Order entity to its REST representation.
     *
     * @param order the persisted domain entity
     * @return a fully populated OrderResponse DTO
     */
    public static OrderResponse fromEntity(Order order) {
        List<OrderItemResponse> itemDtos = order.getItems().stream()
                .map(OrderResponse::mapItem)
                .collect(Collectors.toList());

        return OrderResponse.builder()
                .id(order.getId())
                .userId(order.getUserId())
                .status(order.getStatus().name())
                .totalAmount(order.getTotalAmount())
                .items(itemDtos)
                .createdAt(order.getCreatedAt())
                .updatedAt(order.getUpdatedAt())
                .build();
    }

    private static OrderItemResponse mapItem(OrderItem item) {
        return OrderItemResponse.builder()
                .id(item.getId())
                .productId(item.getProductId())
                .quantity(item.getQuantity())
                .unitPrice(item.getUnitPrice())
                .build();
    }
}
