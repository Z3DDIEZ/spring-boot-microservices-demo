package com.meridian.analyticsservice.application;

import com.meridian.analyticsservice.domain.InventoryMetric;
import com.meridian.analyticsservice.domain.OrderMetric;
import com.meridian.shared.domain.event.InventoryReservedEvent;
import com.meridian.shared.domain.event.OrderCreatedEvent;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AnalyticsServiceTest {

    @Mock
    private MetricsWriter metricsWriter;

    @Mock
    private MetricsReader metricsReader;

    @InjectMocks
    private AnalyticsService analyticsService;

    @Captor
    private ArgumentCaptor<OrderMetric> orderMetricCaptor;

    @Captor
    private ArgumentCaptor<InventoryMetric> inventoryMetricCaptor;

    @Test
    void recordOrderMetric_ShouldMapEventAndDelegateToWriter() {
        // Arrange
        UUID orderId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        BigDecimal total = new BigDecimal("149.99");

        OrderCreatedEvent event = OrderCreatedEvent.builder()
                .orderId(orderId)
                .userId(userId)
                .totalAmount(total)
                .items(List.of(
                        OrderCreatedEvent.OrderItemPayload.builder()
                                .productId(UUID.randomUUID())
                                .quantity(2)
                                .build()
                ))
                .timestamp(LocalDateTime.of(2026, 3, 2, 12, 0))
                .build();

        // Act
        analyticsService.recordOrderMetric(event);

        // Assert
        verify(metricsWriter).writeOrderMetric(orderMetricCaptor.capture());
        OrderMetric captured = orderMetricCaptor.getValue();

        assertThat(captured.getOrderId()).isEqualTo(orderId);
        assertThat(captured.getUserId()).isEqualTo(userId);
        assertThat(captured.getTotalAmount()).isEqualByComparingTo(total);
        assertThat(captured.getItemCount()).isEqualTo(1);
        assertThat(captured.getTimestamp()).isNotNull();
    }

    @Test
    void recordOrderMetric_WithNullItems_ShouldDefaultToZeroCount() {
        // Arrange
        OrderCreatedEvent event = OrderCreatedEvent.builder()
                .orderId(UUID.randomUUID())
                .userId(UUID.randomUUID())
                .totalAmount(BigDecimal.ZERO)
                .items(null)
                .timestamp(LocalDateTime.now())
                .build();

        // Act
        analyticsService.recordOrderMetric(event);

        // Assert
        verify(metricsWriter).writeOrderMetric(orderMetricCaptor.capture());
        assertThat(orderMetricCaptor.getValue().getItemCount()).isZero();
    }

    @Test
    void recordInventoryMetric_ShouldMapEventAndDelegateToWriter() {
        // Arrange
        UUID orderId = UUID.randomUUID();
        UUID productId = UUID.randomUUID();

        InventoryReservedEvent event = InventoryReservedEvent.builder()
                .orderId(orderId)
                .productId(productId)
                .quantity(5)
                .reservationStatus("CONFIRMED")
                .timestamp(LocalDateTime.of(2026, 3, 2, 12, 0))
                .build();

        // Act
        analyticsService.recordInventoryMetric(event);

        // Assert
        verify(metricsWriter).writeInventoryMetric(inventoryMetricCaptor.capture());
        InventoryMetric captured = inventoryMetricCaptor.getValue();

        assertThat(captured.getOrderId()).isEqualTo(orderId);
        assertThat(captured.getProductId()).isEqualTo(productId);
        assertThat(captured.getQuantity()).isEqualTo(5);
        assertThat(captured.getReservationStatus()).isEqualTo("CONFIRMED");
    }

    @Test
    void getOrderMetrics_ShouldDelegateToReader() {
        // Arrange
        Instant start = Instant.parse("2026-03-01T00:00:00Z");
        Instant stop = Instant.parse("2026-03-02T00:00:00Z");
        List<OrderMetric> expected = List.of(OrderMetric.builder().build());
        when(metricsReader.getOrderMetrics(start, stop)).thenReturn(expected);

        // Act
        List<OrderMetric> result = analyticsService.getOrderMetrics(start, stop);

        // Assert
        assertThat(result).isEqualTo(expected);
        verify(metricsReader).getOrderMetrics(start, stop);
    }

    @Test
    void getOrderCount_ShouldDelegateToReader() {
        // Arrange
        Instant start = Instant.parse("2026-03-01T00:00:00Z");
        Instant stop = Instant.parse("2026-03-02T00:00:00Z");
        when(metricsReader.getOrderCount(start, stop)).thenReturn(42L);

        // Act
        long count = analyticsService.getOrderCount(start, stop);

        // Assert
        assertThat(count).isEqualTo(42L);
    }

    @Test
    void getAverageOrderValue_ShouldDelegateToReader() {
        // Arrange
        Instant start = Instant.parse("2026-03-01T00:00:00Z");
        Instant stop = Instant.parse("2026-03-02T00:00:00Z");
        when(metricsReader.getAverageOrderValue(start, stop)).thenReturn(99.50);

        // Act
        double avg = analyticsService.getAverageOrderValue(start, stop);

        // Assert
        assertThat(avg).isEqualTo(99.50);
    }
}
