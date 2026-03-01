# Order Service Mapping Guide

This document outlines where to find key implementations and concepts within the `order-service` module to assist with code reviews and navigation.

## Domain Models & Events

The `domain` package holds pure Java entities representing our business logic, isolated from external frameworks where possible.

- **`Order.java` & `OrderItem.java`**: The core entities mapped to PostgreSQL. They handle the physical state of an order, the list of items, and optimistic locking annotations (`@Version`).
- **`OrderStatus.java`**: Enum tracking the lifecycle of an order (PENDING, CONFIRMED, CANCELLED, etc.).
- **`OutboxEvent.java`**: The entity enabling the Transactional Outbox pattern. When an order is created, a serialized event is saved here in the exact same database transaction.
- **`DomainEvent.java` & `OrderCreatedEvent.java`**: The POJOs representing the events broadcasted to the rest of the system via RabbitMQ.

## Application Logic & Orchestration

The `application` package handles cross-cutting concerns, transaction boundaries, and guaranteed delivery coordination.

- **`OrderService.java`**: The primary `@Transactional` boundary. The `createOrder` method calculates totals, saves the `Order` entity, and serializes the action into an `OutboxEvent`, saving both to PostgreSQL atomically at the exact same time.
- **`OutboxScheduler.java`**: A `@Scheduled` routine that wakes up every 2 seconds, queries the database for unpublished `OutboxEvent`s, and pushes them to RabbitMQ. Ensures guaranteed at-least-once message delivery even if RabbitMQ crashes.

## Infrastructure & Messaging

The `infrastructure` package binds our application to the actual database and Message Broker.

- **`OrderRepository.java` & `OutboxEventRepository.java`**: Spring Data JPA interfaces for native DB queries.
- **`RabbitMQConfig.java`**: Configures the AMQP components: `orders.exchange` (Topic), `order.created.queue`, and the binding linking them together.
- **`JacksonConfig.java`**: Configures the JSON serializer to properly format standard `LocalDateTime` objects.
- **`EventPublisher.java`**: A clean wrapper abstracting the `RabbitTemplate`, exposing a simple `publishEvent(type, payload)` method to the application layer.

## Presentation & API

The `presentation` package exposes the HTTP surface area.

- **`OrderController.java`**: The `@RestController` mapped to `/api/v1/orders`. Exposes `POST /` (create order, 201), `GET /` (list authenticated user's orders), and `GET /{id}` (single order by ID, 404 if missing). Extracts `userId` from the JWT subject claim via `@AuthenticationPrincipal Jwt`.
- **`GlobalExceptionHandler.java`**: A `@RestControllerAdvice` providing consistent JSON error shapes for validation failures (400), not-found (404), and unexpected errors (500).
- **`dto/CreateOrderRequest.java`**: Incoming DTO with Bean Validation (`@NotEmpty`, `@Min`, `@DecimalMin`) for order creation requests.
- **`dto/OrderResponse.java`**: Outgoing DTO with a `fromEntity(Order)` factory method mapping domain entities to REST payloads.
- **`dto/OrderItemResponse.java`**: Outgoing DTO representing a single line item.

## Security

- **`SecurityConfig.java`**: Configures the Order Service as a stateless OAuth2 Resource Server (JWT). Actuator health endpoints are open; all `/api/**` routes require authentication.
