package com.meridian.analyticsservice.application;

import com.meridian.analyticsservice.domain.InventoryMetric;
import com.meridian.analyticsservice.domain.OrderMetric;

import java.time.Instant;
import java.util.List;

/**
 * Architectural inbound port explicitly defining the contract for executing historical 
 * queries against the underlying time-series data store (InfluxDB).
 */
public interface MetricsReader {

    List<OrderMetric> getOrderMetrics(Instant start, Instant stop);

    List<InventoryMetric> getInventoryMetrics(Instant start, Instant stop);

    long getOrderCount(Instant start, Instant stop);

    double getAverageOrderValue(Instant start, Instant stop);
}
