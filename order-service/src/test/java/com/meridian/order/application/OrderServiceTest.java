package com.meridian.order.application;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.meridian.order.domain.Order;
import com.meridian.order.domain.OrderStatus;
import com.meridian.order.infrastructure.OrderRepository;
import com.meridian.order.infrastructure.OutboxEventRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock
    private OrderRepository orderRepository;
    @Mock
    private OutboxEventRepository outboxEventRepository;
    @Mock
    private ObjectMapper objectMapper;

    private OrderService orderService;

    @BeforeEach
    void setUp() {
        orderService = new OrderService(orderRepository, outboxEventRepository, objectMapper);
    }

    @Test
    void createOrder_Success() throws Exception {
        // Arrange
        UUID userId = UUID.randomUUID();
        UUID productId1 = UUID.randomUUID();
        UUID productId2 = UUID.randomUUID();

        List<OrderService.OrderItemInput> items = List.of(
                new OrderService.OrderItemInput(productId1, 2, BigDecimal.valueOf(10.00)),
                new OrderService.OrderItemInput(productId2, 1, BigDecimal.valueOf(25.50)));

        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> {
            Order order = invocation.getArgument(0);
            order.setId(UUID.randomUUID());
            return order;
        });

        when(objectMapper.writeValueAsString(any())).thenReturn("{\"mock\":\"payload\"}");

        // Act
        Order result = orderService.createOrder(userId, items);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getId()).isNotNull();
        assertThat(result.getUserId()).isEqualTo(userId);
        assertThat(result.getItems()).hasSize(2);

        // Verify total: (2 * 10.00) + (1 * 25.50) = 45.50
        assertThat(result.getTotalAmount()).isEqualByComparingTo(BigDecimal.valueOf(45.50));

        // Verify outbox event was persisted
        verify(outboxEventRepository, times(1)).save(any());
        verify(orderRepository, times(1)).save(any(Order.class));
    }

    @Test
    void createOrder_SerializationFailure_ThrowsException() throws Exception {
        // Arrange
        UUID userId = UUID.randomUUID();
        List<OrderService.OrderItemInput> items = List.of(
                new OrderService.OrderItemInput(UUID.randomUUID(), 1, BigDecimal.TEN));

        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> {
            Order order = invocation.getArgument(0);
            order.setId(UUID.randomUUID());
            return order;
        });

        when(objectMapper.writeValueAsString(any()))
                .thenThrow(new JsonProcessingException("Simulated failure") {
                });

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> orderService.createOrder(userId, items));

        assertThat(exception.getMessage()).contains("Serialization failure");
        verify(outboxEventRepository, never()).save(any());
    }

    @Test
    void getOrderById_Found() {
        // Arrange
        UUID orderId = UUID.randomUUID();
        Order order = new Order();
        order.setId(orderId);
        order.setStatus(OrderStatus.PENDING);

        when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));

        // Act
        Optional<Order> result = orderService.getOrderById(orderId);

        // Assert
        assertThat(result).isPresent();
        assertThat(result.get().getId()).isEqualTo(orderId);
        verify(orderRepository, times(1)).findById(orderId);
    }

    @Test
    void getOrderById_NotFound() {
        // Arrange
        UUID orderId = UUID.randomUUID();
        when(orderRepository.findById(orderId)).thenReturn(Optional.empty());

        // Act
        Optional<Order> result = orderService.getOrderById(orderId);

        // Assert
        assertThat(result).isEmpty();
        verify(orderRepository, times(1)).findById(orderId);
    }

    @Test
    void getOrdersByUserId_ReturnsOrders() {
        // Arrange
        UUID userId = UUID.randomUUID();
        Order order1 = new Order();
        order1.setId(UUID.randomUUID());
        order1.setUserId(userId);

        Order order2 = new Order();
        order2.setId(UUID.randomUUID());
        order2.setUserId(userId);

        when(orderRepository.findByUserIdOrderByCreatedAtDesc(userId))
                .thenReturn(List.of(order1, order2));

        // Act
        List<Order> results = orderService.getOrdersByUserId(userId);

        // Assert
        assertThat(results).hasSize(2);
        verify(orderRepository, times(1)).findByUserIdOrderByCreatedAtDesc(userId);
    }
}
