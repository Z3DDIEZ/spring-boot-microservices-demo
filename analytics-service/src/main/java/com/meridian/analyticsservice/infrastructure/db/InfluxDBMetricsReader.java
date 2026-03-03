package com.meridian.analyticsservice.infrastructure.db;

import com.influxdb.client.InfluxDBClient;
import com.influxdb.client.QueryApi;
import com.influxdb.query.FluxRecord;
import com.influxdb.query.FluxTable;
import com.meridian.analyticsservice.application.MetricsReader;
import com.meridian.analyticsservice.domain.InventoryMetric;
import com.meridian.analyticsservice.domain.OrderMetric;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Concrete implementation of the {@link MetricsReader} architectural port.
 * <p>
 * Translates abstract, domain-agnostic query parameters into highly optimized
 * native InfluxDB Flux language queries, extracting and hydrating clean domain
 * objects.
 */
@Component
@Slf4j
public class InfluxDBMetricsReader implements MetricsReader {

    private final InfluxDBClient influxDBClient;
    private final String bucket;

    public InfluxDBMetricsReader(InfluxDBClient influxDBClient,
            @Qualifier("influxBucket") String bucket) {
        this.influxDBClient = influxDBClient;
        this.bucket = bucket;
    }

    @Override
    public List<OrderMetric> getOrderMetrics(Instant start, Instant stop) {
        String flux = String.format(
                "from(bucket: \"%s\")" +
                        " |> range(start: %s, stop: %s)" +
                        " |> filter(fn: (r) => r._measurement == \"order_metrics\")" +
                        " |> pivot(rowKey: [\"_time\"], columnKey: [\"_field\"], valueColumn: \"_value\")",
                bucket, start.toString(), stop.toString());

        log.debug("Executing Flux query for order metrics: {}", flux);

        QueryApi queryApi = influxDBClient.getQueryApi();
        List<FluxTable> tables = queryApi.query(flux);
        List<OrderMetric> metrics = new ArrayList<>();

        for (FluxTable table : tables) {
            for (FluxRecord record : table.getRecords()) {
                OrderMetric metric = OrderMetric.builder()
                        .orderId(UUID.fromString((String) record.getValueByKey("orderId")))
                        .userId(UUID.fromString((String) record.getValueByKey("userId")))
                        .totalAmount(BigDecimal.valueOf((Double) record.getValueByKey("totalAmount")))
                        .itemCount(((Number) record.getValueByKey("itemCount")).intValue())
                        .timestamp(record.getTime())
                        .build();
                metrics.add(metric);
            }
        }

        return metrics;
    }

    @Override
    public List<InventoryMetric> getInventoryMetrics(Instant start, Instant stop) {
        String flux = String.format(
                "from(bucket: \"%s\")" +
                        " |> range(start: %s, stop: %s)" +
                        " |> filter(fn: (r) => r._measurement == \"inventory_metrics\")" +
                        " |> pivot(rowKey: [\"_time\"], columnKey: [\"_field\"], valueColumn: \"_value\")",
                bucket, start.toString(), stop.toString());

        log.debug("Executing Flux query for inventory metrics: {}", flux);

        QueryApi queryApi = influxDBClient.getQueryApi();
        List<FluxTable> tables = queryApi.query(flux);
        List<InventoryMetric> metrics = new ArrayList<>();

        for (FluxTable table : tables) {
            for (FluxRecord record : table.getRecords()) {
                InventoryMetric metric = InventoryMetric.builder()
                        .orderId(UUID.fromString((String) record.getValueByKey("orderId")))
                        .productId(UUID.fromString((String) record.getValueByKey("productId")))
                        .quantity(((Number) record.getValueByKey("quantity")).intValue())
                        .reservationStatus((String) record.getValueByKey("reservationStatus"))
                        .timestamp(record.getTime())
                        .build();
                metrics.add(metric);
            }
        }

        return metrics;
    }

    @Override
    public long getOrderCount(Instant start, Instant stop) {
        String flux = String.format(
                "from(bucket: \"%s\")" +
                        " |> range(start: %s, stop: %s)" +
                        " |> filter(fn: (r) => r._measurement == \"order_metrics\" and r._field == \"totalAmount\")" +
                        " |> count()",
                bucket, start.toString(), stop.toString());

        log.debug("Executing Flux query for order count: {}", flux);

        QueryApi queryApi = influxDBClient.getQueryApi();
        List<FluxTable> tables = queryApi.query(flux);

        if (!tables.isEmpty() && !tables.get(0).getRecords().isEmpty()) {
            return ((Number) tables.get(0).getRecords().get(0).getValue()).longValue();
        }

        return 0L;
    }

    @Override
    public double getAverageOrderValue(Instant start, Instant stop) {
        String flux = String.format(
                "from(bucket: \"%s\")" +
                        " |> range(start: %s, stop: %s)" +
                        " |> filter(fn: (r) => r._measurement == \"order_metrics\" and r._field == \"totalAmount\")" +
                        " |> mean()",
                bucket, start.toString(), stop.toString());

        log.debug("Executing Flux query for average order value: {}", flux);

        QueryApi queryApi = influxDBClient.getQueryApi();
        List<FluxTable> tables = queryApi.query(flux);

        if (!tables.isEmpty() && !tables.get(0).getRecords().isEmpty()) {
            Object value = tables.get(0).getRecords().get(0).getValue();
            return (value instanceof Number) ? ((Number) value).doubleValue() : 0.0;
        }

        return 0.0;
    }
}
