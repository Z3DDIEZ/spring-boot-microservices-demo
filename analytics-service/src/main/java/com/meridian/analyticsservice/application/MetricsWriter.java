package com.meridian.analyticsservice.application;

import com.meridian.analyticsservice.domain.InventoryMetric;
import com.meridian.analyticsservice.domain.OrderMetric;

/**
 * Port interface for writing metric data points to the time-series store.
 * Implemented by an InfluxDB adapter in the infrastructure layer.
 */
public interface MetricsWriter {

    void writeOrderMetric(OrderMetric metric);

    void writeInventoryMetric(InventoryMetric metric);
}
