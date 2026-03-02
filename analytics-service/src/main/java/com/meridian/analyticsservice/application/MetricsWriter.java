package com.meridian.analyticsservice.application;

import com.meridian.analyticsservice.domain.InventoryMetric;
import com.meridian.analyticsservice.domain.OrderMetric;

/**
 * Architectural outbound port explicitly defining the contract for safely trickling 
 * continuous metric data points into the underlying time-series data store (InfluxDB).
 */
public interface MetricsWriter {

    void writeOrderMetric(OrderMetric metric);

    void writeInventoryMetric(InventoryMetric metric);
}
