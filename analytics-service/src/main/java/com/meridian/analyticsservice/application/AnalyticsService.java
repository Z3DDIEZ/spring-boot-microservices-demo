package com.meridian.analyticsservice.application;

import com.meridian.analyticsservice.domain.InventoryMetric;
import com.meridian.analyticsservice.domain.OrderMetric;
import com.meridian.shared.domain.event.InventoryReservedEvent;
import com.meridian.shared.domain.event.OrderCreatedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;

/**
 * Centralized Application Use Case orchestrator for metric aggregation.
 * <p>
 * Receives primitive domain events originating from detached RabbitMQ consumers, translates them 
 * into bounded Context domain metrics, and immediately delegates to the {@link MetricsWriter} port.
 * Exposes mirrored query delegation methods leveraging the {@link MetricsReader} port.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AnalyticsService {

    private final MetricsWriter metricsWriter;
    private final MetricsReader metricsReader;

    // --- Write Use Cases (invoked by RabbitMQ consumers) ---

    /**
     * Deconstructs a broadcast 'OrderCreated' payload into a flattened timeseries metric.
     * Enforces the translation of local Server times into strictly bounded UTC Instants.
     *
     * @param event The volatile event originating from the distributed message broker.
     */
    public void recordOrderMetric(OrderCreatedEvent event) {
        log.info("Recording order metric for order: {}", event.getOrderId());

        int itemCount = (event.getItems() != null) ? event.getItems().size() : 0;

        OrderMetric metric = OrderMetric.builder()
                .orderId(event.getOrderId())
                .userId(event.getUserId())
                .totalAmount(event.getTotalAmount())
                .itemCount(itemCount)
                .timestamp(event.getTimestamp().toInstant(ZoneOffset.UTC))
                .build();

        metricsWriter.writeOrderMetric(metric);
    }

    /**
     * Deconstructs a broadcast 'InventoryReserved' payload into a flattened timeseries metric.
     * Enforces the translation of local Server times into strictly bounded UTC Instants.
     *
     * @param event The volatile event originating from the distributed message broker.
     */
    public void recordInventoryMetric(InventoryReservedEvent event) {
        log.info("Recording inventory metric for order: {}, product: {}",
                event.getOrderId(), event.getProductId());

        InventoryMetric metric = InventoryMetric.builder()
                .orderId(event.getOrderId())
                .productId(event.getProductId())
                .quantity(event.getQuantity())
                .reservationStatus(event.getReservationStatus())
                .timestamp(event.getTimestamp().toInstant(ZoneOffset.UTC))
                .build();

        metricsWriter.writeInventoryMetric(metric);
    }

    // --- Read Use Cases (invoked by GraphQL resolvers) ---

    public List<OrderMetric> getOrderMetrics(Instant start, Instant stop) {
        return metricsReader.getOrderMetrics(start, stop);
    }

    public List<InventoryMetric> getInventoryMetrics(Instant start, Instant stop) {
        return metricsReader.getInventoryMetrics(start, stop);
    }

    public long getOrderCount(Instant start, Instant stop) {
        return metricsReader.getOrderCount(start, stop);
    }

    public double getAverageOrderValue(Instant start, Instant stop) {
        return metricsReader.getAverageOrderValue(start, stop);
    }
}
