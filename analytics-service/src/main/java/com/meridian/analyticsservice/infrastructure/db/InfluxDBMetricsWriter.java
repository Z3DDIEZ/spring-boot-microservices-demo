package com.meridian.analyticsservice.infrastructure.db;

import com.influxdb.client.InfluxDBClient;
import com.influxdb.client.WriteApiBlocking;
import com.influxdb.client.domain.WritePrecision;
import com.influxdb.client.write.Point;
import com.meridian.analyticsservice.application.MetricsWriter;
import com.meridian.analyticsservice.domain.InventoryMetric;
import com.meridian.analyticsservice.domain.OrderMetric;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Implements the MetricsWriter port using InfluxDB's WriteApiBlocking.
 * Each metric is written as a Point with tags (indexed) and fields (values).
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class InfluxDBMetricsWriter implements MetricsWriter {

    private final InfluxDBClient influxDBClient;

    @Override
    public void writeOrderMetric(OrderMetric metric) {
        log.debug("Writing order metric to InfluxDB: orderId={}", metric.getOrderId());

        Point point = Point.measurement("order_metrics")
                .addTag("orderId", metric.getOrderId().toString())
                .addTag("userId", metric.getUserId().toString())
                .addField("totalAmount", metric.getTotalAmount().doubleValue())
                .addField("itemCount", metric.getItemCount())
                .time(metric.getTimestamp(), WritePrecision.MS);

        WriteApiBlocking writeApi = influxDBClient.getWriteApiBlocking();
        writeApi.writePoint(point);
    }

    @Override
    public void writeInventoryMetric(InventoryMetric metric) {
        log.debug("Writing inventory metric to InfluxDB: productId={}", metric.getProductId());

        Point point = Point.measurement("inventory_metrics")
                .addTag("orderId", metric.getOrderId().toString())
                .addTag("productId", metric.getProductId().toString())
                .addTag("reservationStatus", metric.getReservationStatus())
                .addField("quantity", metric.getQuantity())
                .time(metric.getTimestamp(), WritePrecision.MS);

        WriteApiBlocking writeApi = influxDBClient.getWriteApiBlocking();
        writeApi.writePoint(point);
    }
}
