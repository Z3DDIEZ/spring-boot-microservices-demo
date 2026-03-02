package com.meridian.analyticsservice.infrastructure.graphql;

import com.meridian.analyticsservice.application.AnalyticsService;
import com.meridian.analyticsservice.domain.InventoryMetric;
import lombok.RequiredArgsConstructor;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.stereotype.Controller;

import java.time.Instant;
import java.util.List;

/**
 * GraphQL resolver for inventory-related metric queries.
 * Acts as the presentation boundary for inventory analytics data.
 */
@Controller
@RequiredArgsConstructor
public class InventoryMetricsResolver {

    private final AnalyticsService analyticsService;

    @QueryMapping
    public List<InventoryMetric> inventoryMetrics(@Argument String start, @Argument String stop) {
        return analyticsService.getInventoryMetrics(Instant.parse(start), Instant.parse(stop));
    }
}
