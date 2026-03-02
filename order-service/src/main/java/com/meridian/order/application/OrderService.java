package com.meridian.order.application;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.meridian.order.domain.Order;
import com.meridian.order.domain.OrderItem;
import com.meridian.order.domain.OutboxEvent;
import com.meridian.shared.domain.event.OrderCreatedEvent;
import com.meridian.order.infrastructure.OrderRepository;
import com.meridian.order.infrastructure.OutboxEventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Core Application Service orchestrating complex business use cases for Orders.
 * <p>
 * This boundary class manages transactional integrity across the internal Order
 * state
 * and the outbox pattern. It translates raw DTO inputs into domain concepts and
 * guarantees
 * domain events are safely staged for future broadcast without distributed
 * transaction overhead.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class OrderService {

    private final OrderRepository orderRepository;
    private final OutboxEventRepository outboxEventRepository;
    private final ObjectMapper objectMapper;

    /**
     * Creates a new Order, saves it to the database, and safely persists the
     * corresponding
     * {@link OrderCreatedEvent} into the Outbox table within a single atomic
     * database transaction.
     * <p>
     * This achieves strict eventual consistency with downstream microservices (like
     * Inventory).
     *
     * @param userId       the authenticated logical user placing the order.
     * @param itemRequests the requested line items containing product IDs,
     *                     quantities, and prices.
     * @return the completely persisted and versioned Order entity.
     * @throws RuntimeException if Jackson fails to serialize the outbox payload.
     */
    @Transactional
    public Order createOrder(UUID userId, List<OrderItemInput> itemRequests) {
        log.info("Creating new order for user {}", userId);

        // 1. Calculate totals and build OrderItems
        BigDecimal totalAmount = BigDecimal.ZERO;
        Order order = new Order();
        order.setUserId(userId);

        for (OrderItemInput req : itemRequests) {
            BigDecimal itemTotal = req.unitPrice().multiply(BigDecimal.valueOf(req.quantity()));
            totalAmount = totalAmount.add(itemTotal);

            OrderItem item = OrderItem.builder()
                    .productId(req.productId())
                    .quantity(req.quantity())
                    .unitPrice(req.unitPrice())
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

    /**
     * Retrieves a single order by its ID.
     *
     * @param orderId the UUID of the order
     * @return an Optional containing the order if found, empty otherwise
     */
    @Transactional(readOnly = true)
    public Optional<Order> getOrderById(UUID orderId) {
        return orderRepository.findById(orderId);
    }

    /**
     * Retrieves all orders belonging to a specific user, sorted newest first.
     *
     * @param userId the UUID of the user
     * @return a list of orders for the given user
     */
    @Transactional(readOnly = true)
    public List<Order> getOrdersByUserId(UUID userId) {
        return orderRepository.findByUserIdOrderByCreatedAtDesc(userId);
    }

    /**
     * Immutable input record used by the controller to pass item data
     * into the application layer without coupling to presentation DTOs.
     */
    public record OrderItemInput(UUID productId, Integer quantity, BigDecimal unitPrice) {
    }
}
