# Technology Stack

## Core Framework & Build Tools

```yaml
Language: Java 21 LTS (Amazon Corretto or Eclipse Temurin)
Framework: Spring Boot 3.5.x
Build Tool: Maven 3.9.x
Dependency Management: Spring Boot Starter Parent
```

## Spring Ecosystem

```yaml
Spring Boot Starters:
  - spring-boot-starter-web (REST APIs)
  - spring-boot-starter-data-jpa (PostgreSQL access)
  - spring-boot-starter-data-mongodb (MongoDB access)
  - spring-boot-starter-data-redis (Redis caching)
  - spring-boot-starter-security (Security framework)
  - spring-boot-starter-oauth2-resource-server (JWT validation)
  - spring-boot-starter-amqp (RabbitMQ messaging)
  - spring-boot-starter-actuator (Health checks, metrics)
  - spring-boot-starter-validation (Bean validation)

Spring Cloud:
  - spring-cloud-starter-gateway (API Gateway)
  - spring-cloud-starter-circuitbreaker-resilience4j (Circuit breakers)

Spring Security:
  - spring-security-oauth2-authorization-server (OAuth2 server)
```

## Databases & Data Stores

```yaml
Transactional Databases:
  PostgreSQL: 18.x
    - Auth Service: Users, roles, tokens
    - Order Service: Orders, order_items, outbox_events

Document Database:
  MongoDB: 8.0.x
    - Inventory Service: Products (flexible schema)

Caching Layer:
  Redis: 8.x (Alpine)
    - API Gateway: Rate limiting token buckets
    - Order Service: Session caching

Time-Series Database:
  InfluxDB: 2.8.x
    - Analytics Service: Metrics aggregation
```

## Message Broker

```yaml
Message Broker: RabbitMQ 4.2.x
  Exchanges:
    - orders.exchange (topic, durable)
    - inventory.exchange (topic, durable)
    - notifications.exchange (fanout, durable)

  Queues:
    - order.created.queue (bound to orders.exchange)
    - inventory.reserved.queue (bound to inventory.exchange)
    - notification.email.queue (bound to notifications.exchange)
    - notification.sms.queue (bound to notifications.exchange)
    - dlq.orders (dead letter queue)
    - dlq.inventory (dead letter queue)
```

## Observability Stack

```yaml
Distributed Tracing:
  Jaeger: 2.15.x
  OpenTelemetry Java Agent: 2.25.x

Metrics & Monitoring:
  Prometheus: 3.5.x
  Grafana: 12.4.x
  Micrometer: (included in Spring Boot)

Logging: SLF4J + Logback (Spring Boot default)
```
