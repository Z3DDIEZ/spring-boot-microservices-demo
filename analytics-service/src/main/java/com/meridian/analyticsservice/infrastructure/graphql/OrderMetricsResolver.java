package com.meridian.analyticsservice.infrastructure.graphql;

import com.meridian.analyticsservice.application.AnalyticsService;
import com.meridian.analyticsservice.domain.OrderMetric;
import lombok.RequiredArgsConstructor;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.stereotype.Controller;

import java.time.Instant;
import java.util.List;

/**
 * Presentation layer GraphQL Controller specifically managing financial order
 * aggregations.
 * Maps schema definition queries directly into the core Application Use Cases.
 */
@Controller
@RequiredArgsConstructor
public class OrderMetricsResolver {

    private final AnalyticsService analyticsService;

    @QueryMapping
    public List<OrderMetric> orderMetrics(@Argument String start, @Argument String stop) {
        return analyticsService.getOrderMetrics(Instant.parse(start), Instant.parse(stop));
    }

    @QueryMapping
    public double orderCount(@Argument String start, @Argument String stop) {
        return (double) analyticsService.getOrderCount(Instant.parse(start), Instant.parse(stop));
    }

    @QueryMapping
    public double averageOrderValue(@Argument String start, @Argument String stop) {
        return analyticsService.getAverageOrderValue(Instant.parse(start), Instant.parse(stop));
    }
}
