package com.meridian.analyticsservice.application;

import com.meridian.analyticsservice.domain.InventoryMetric;
import com.meridian.analyticsservice.domain.OrderMetric;

import java.time.Instant;
import java.util.List;

/**
 * Port interface for reading metric data points from the time-series store.
 * Implemented by an InfluxDB adapter in the infrastructure layer.
 */
public interface MetricsReader {

    List<OrderMetric> getOrderMetrics(Instant start, Instant stop);

    List<InventoryMetric> getInventoryMetrics(Instant start, Instant stop);

    long getOrderCount(Instant start, Instant stop);

    double getAverageOrderValue(Instant start, Instant stop);
}
