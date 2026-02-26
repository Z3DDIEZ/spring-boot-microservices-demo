package com.meridian.order.application;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.meridian.order.domain.Order;
import com.meridian.order.domain.OrderCreatedEvent;
import com.meridian.order.domain.OrderItem;
import com.meridian.order.domain.OutboxEvent;
import com.meridian.order.infrastructure.OrderRepository;
import com.meridian.order.infrastructure.OutboxEventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrderService {

    private final OrderRepository orderRepository;
    private final OutboxEventRepository outboxEventRepository;
    private final ObjectMapper objectMapper;

    /**
     * Creates a new Order, saves it to the DB, and generates the corresponding DomainEvent
     * into the Outbox table within a single atomic transaction.
     */
    @Transactional
    public Order createOrder(UUID userId, List<OrderItemRequest> itemRequests) {
        log.info("Creating new order for user {}", userId);

        // 1. Calculate totals and build OrderItems
        BigDecimal totalAmount = BigDecimal.ZERO;
        Order order = new Order();
        order.setUserId(userId);

        for (OrderItemRequest req : itemRequests) {
            BigDecimal itemTotal = req.getUnitPrice().multiply(BigDecimal.valueOf(req.getQuantity()));
            totalAmount = totalAmount.add(itemTotal);

            OrderItem item = OrderItem.builder()
                    .productId(req.getProductId())
                    .quantity(req.getQuantity())
                    .unitPrice(req.getUnitPrice())
                    .build();
            
            order.addItem(item);
        }

        order.setTotalAmount(totalAmount);

        // 2. Persist Order to DB (Gets ID)
        Order savedOrder = orderRepository.save(order);

        // 3. Build the Domain Event
        List<OrderCreatedEvent.OrderItemPayload> eventItems = savedOrder.getItems().stream()
                .map(i -> new OrderCreatedEvent.OrderItemPayload(i.getProductId(), i.getQuantity()))
                .collect(Collectors.toList());

        OrderCreatedEvent event = OrderCreatedEvent.builder()
                .orderId(savedOrder.getId())
                .userId(savedOrder.getUserId())
                .items(eventItems)
                .totalAmount(savedOrder.getTotalAmount())
                .build();

        // 4. Save Event to Outbox Table
        try {
            OutboxEvent outbox = OutboxEvent.builder()
                    .aggregateId(savedOrder.getId().toString())
                    .aggregateType("Order")
                    .eventType(event.getEventType())
                    .payload(objectMapper.writeValueAsString(event))
                    .build();

            outboxEventRepository.save(outbox);
            log.info("Persisted OutboxEvent for Order {}", savedOrder.getId());
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize OrderCreatedEvent payload", e);
            throw new RuntimeException("Serialization failure during order creation");
        }

        return savedOrder;
    }

    // A simple DTO wrapper for internal passing, normally this lives in presentation layer 
    // but we use it here to decouple the HTTP request from the exact domain shape.
    @lombok.Data
    @lombok.AllArgsConstructor
    @lombok.NoArgsConstructor
    public static class OrderItemRequest {
        private UUID productId;
        private Integer quantity;
        private BigDecimal unitPrice;
    }
}
