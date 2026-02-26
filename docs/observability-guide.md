# Observability Guide

## OpenTelemetry Configuration

We use the OpenTelemetry Java Agent to automatically instrument all Spring Boot microservices.

**Java Agent Attachment** (all services)

```bash
java -javaagent:/path/to/opentelemetry-javaagent.jar \
     -Dotel.service.name=order-service \
     -Dotel.traces.exporter=jaeger \
     -Dotel.exporter.jaeger.endpoint=http://jaeger:14250 \
     -Dotel.metrics.exporter=prometheus \
     -Dotel.exporter.prometheus.host=0.0.0.0 \
     -Dotel.exporter.prometheus.port=9464 \
     -jar order-service.jar
```

**Spring Boot Configuration** (`application.yml`)

```yaml
management:
  endpoints:
    web:
      exposure:
        include: health,info,prometheus,metrics
  metrics:
    export:
      prometheus:
        enabled: true
    tags:
      application: ${spring.application.name}
  tracing:
    sampling:
      probability: 1.0 # 100% sampling in dev, 0.1 in prod
```

## Distributed Tracing

Requests are traced from the entry point (API Gateway) down through all synchronous calls and asynchronous event messaging.

**Trace Context Propagation**

```
Client → Gateway (Trace ID: abc123)
  ├─→ Auth Service (Trace ID: abc123, Span ID: span1)
  ├─→ Order Service (Trace ID: abc123, Span ID: span2)
  │   └─→ Inventory Service (Trace ID: abc123, Span ID: span3)
  └─→ Notification Service (Trace ID: abc123, Span ID: span4)
```

**Jaeger UI**: Accessible at `http://localhost:16686` during local development.

## Metrics Collection

Prometheus periodically scrapes metrics exposed by the Spring Boot Actuator `/actuator/prometheus` endpoint. Micrometer bridges the metrics to Prometheus.

**Key Metrics** (Prometheus)

```
# Request metrics
http_server_requests_seconds_count{uri="/orders", status="200"}
http_server_requests_seconds_sum{uri="/orders", status="200"}

# JVM metrics
jvm_memory_used_bytes{area="heap"}
jvm_threads_live_threads

# Custom business metrics
orders_created_total{status="CONFIRMED"}
inventory_stock_quantity{product_category="Electronics"}
```

## Grafana Dashboards

Grafana is pre-configured with the following dashboards for a comprehensive view:

- **System Health**: CPU, Memory, Thread count per service
- **Request Rates**: Requests/sec, error rate, latency percentiles
- **Business Metrics**: Orders/hour, revenue/day, low-stock alerts
