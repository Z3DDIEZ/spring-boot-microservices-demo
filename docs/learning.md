# Meridian Backend Engineering Learning Guide

## Introduction

This document is the engineering foundation for the Meridian project. Every major architectural decision, design pattern, framework mechanism, and infrastructure choice is explained here at the level of _why it exists_, not just _what it does_. When you look at a class and ask "why is this structured this way?", this document answers that question with precision.

The Meridian project is not a toy. It deliberately implements patterns that appear in production systems at scale: the Transactional Outbox, reactive API gateways, polyglot persistence, distributed observability, and event-driven inter-service communication. Understanding these patterns deeply — not just being able to write them — is what separates a junior engineer who can follow a tutorial from one who can make architectural decisions under pressure.

Read this document alongside the code. When you add a new class, update this document. When you change a design decision, record why the previous approach was replaced.

---

## Table of Contents

1. [System Architecture — The Big Picture](#1-system-architecture--the-big-picture)
2. [Maven Multi-Module Reactor](#2-maven-multi-module-reactor)
3. [Docker Compose — Infrastructure as Code](#3-docker-compose--infrastructure-as-code)
4. [Architectural Philosophy: Event-Driven Microservices](#4-architectural-philosophy-event-driven-microservices)
5. [The Clean Architecture Paradigm](#5-the-clean-architecture-paradigm)
6. [Auth Service Deep Dive](#6-auth-service-deep-dive)
7. [Order Service Deep Dive](#7-order-service-deep-dive)
8. [API Gateway Deep Dive](#8-api-gateway-deep-dive)
9. [Inventory Service Deep Dive](#9-inventory-service-deep-dive)
10. [Notification Service Deep Dive](#10-notification-service-deep-dive) _(planned)_
11. [Analytics Service Deep Dive](#11-analytics-service-deep-dive) _(planned)_
12. [Observability — Micrometer, Prometheus, and Jaeger](#12-observability--micrometer-prometheus-and-jaeger)
13. [Testing Strategy](#13-testing-strategy)
14. [Security Architecture — Cross-Cutting Concerns](#14-security-architecture--cross-cutting-concerns)
15. [Data Architecture — Polyglot Persistence](#15-data-architecture--polyglot-persistence)
16. [Summary of Completed Code Modules](#16-summary-of-completed-code-modules)

---

## 1. System Architecture — The Big Picture

### What Meridian Is

Meridian is a backend platform composed of six independently deployable services, each owning a specific bounded context:

| Service                | Port | Responsibility                                               | Primary Store            |
| ---------------------- | ---- | ------------------------------------------------------------ | ------------------------ |
| `api-gateway`          | 8080 | Single entry point, routing, rate limiting, circuit breaking | Redis (rate limit state) |
| `auth-service`         | 8081 | Identity, authentication, JWT issuance                       | PostgreSQL               |
| `order-service`        | 8082 | Order lifecycle, domain event publishing                     | PostgreSQL               |
| `inventory-service`    | 8083 | Product catalogue, stock reservation                         | MongoDB                  |
| `notification-service` | 8084 | Async email and push notifications                           | — (stateless consumer)   |
| `analytics-service`    | 8085 | Time-series metrics, GraphQL API                             | InfluxDB                 |

No service calls another service's HTTP API directly. All inter-service communication flows through RabbitMQ. The only exception is the `api-gateway`, whose job _is_ to proxy HTTP traffic to downstream services.

### The Request Lifecycle

A single `POST /api/v1/orders` request travels through Meridian as follows:

```
Client
  → api-gateway (JWT validation, rate limit check, circuit breaker)
  → order-service (REST controller → application service → domain → outbox)
  → PostgreSQL (order + outbox event committed atomically)
  → OutboxScheduler (background thread, polls every 2s)
  → RabbitMQ orders.exchange
  → inventory-service (reserves stock, emits InventoryReservedEvent)
  → notification-service (sends order confirmation email)
  → analytics-service (records order metric in InfluxDB)
```

This is the core event chain. Every design decision in the system serves this flow.

### Why This Architecture Is Hard

The interesting engineering challenges in this architecture are not in the happy path above. They are in the failure paths:

- What happens if the server crashes _after_ writing the order but _before_ publishing to RabbitMQ? (Answered by the Outbox Pattern.)
- What happens if the inventory consumer processes the same message twice? (Answered by idempotency checks.)
- What happens if `order-service` is completely down when the gateway receives a request? (Answered by the circuit breaker.)
- What happens if 10,000 requests/second arrive at the gateway? (Answered by Redis-backed rate limiting.)

Every deep-dive section below explains how one of these failure modes is handled.

---

## 2. Maven Multi-Module Reactor

### What a Multi-Module Maven Project Is

The `pom.xml` at the root of the repository is the **parent POM** — it does not contain application code itself. It declares a list of `<modules>` pointing to each service subdirectory. When you run `mvn compile` at the root, Maven builds every module in the correct dependency order. This is called the **reactor build**.

```
meridian-backend/
  pom.xml              ← parent POM, no source code
  auth-service/
    pom.xml            ← child POM, inherits from parent
  order-service/
    pom.xml            ← child POM, inherits from parent
  shared-lib/
    pom.xml            ← child POM, produces a .jar other services import
```

### Why This Structure Matters

**Dependency management in one place.** The parent POM declares the Spring Boot version, Java version, and all third-party dependency versions inside `<dependencyManagement>`. Child POMs reference `spring-boot-starter-web` without specifying a version — they inherit it from the parent. This means upgrading Spring Boot happens in exactly one file, and every service picks up the change automatically.

**The `shared-lib` module.** _(Status: Planned - Currently being scaffolded, DTOs are temporarily duplicated)_ Rather than duplicating event DTO classes (`OrderCreatedEvent`, `InventoryReservedEvent`) across every service that needs them, `shared-lib` contains these shared types as a plain Java library. `order-service` imports it as a Maven dependency. `inventory-service` imports it too. They now share the exact same class definition — no deserialization mismatches.

### The `<parent>` Declaration in Each Child POM

Every service POM starts with:

```xml
<parent>
    <groupId>com.meridian</groupId>
    <artifactId>meridian-backend</artifactId>
    <version>1.0.0-SNAPSHOT</version>
</parent>
```

This means the child inherits: the Spring Boot plugin configuration, the Java compiler version (set to 21), all managed dependency versions, and any shared plugin configurations (like Surefire for testing). Without this inheritance, every service would need to repeat hundreds of lines of POM configuration.

---

## 3. Docker Compose — Infrastructure as Code

### What `docker-compose.yml` Does

The `docker-compose.yml` at the root defines all infrastructure dependencies as reproducible containers. When you run `docker compose up -d auth-db order-db rabbitmq`, Docker pulls the correct images and starts PostgreSQL and RabbitMQ with the exact configuration the services expect — no local installation of PostgreSQL required, no version mismatch with a colleague's machine.

### Service Definitions — What to Know

**PostgreSQL containers (`auth-db`, `order-db`)**: Each service gets its own database container, not a shared one. This enforces the microservices principle that services do not share databases. `auth-service` can only reach `auth-db`. `order-service` can only reach `order-db`. If they shared a database, a schema migration in one service could break another — that coupling defeats the purpose of microservices.

**RabbitMQ container**: Exposes two ports — `5672` (the AMQP protocol port that services use to publish/consume messages) and `15672` (the management UI). During development, navigate to `http://localhost:15672` with credentials `guest/guest` to watch messages flow through exchanges and queues in real time. This is invaluable for debugging event-driven flows.

**Redis container**: Used exclusively by the API gateway for rate limit state. Redis is chosen over an in-memory store because it is shared across all gateway instances — if you run three gateway instances behind a load balancer, all three must consult the same rate limit counters or the limits are meaningless.

### Docker Networking

All containers in a `docker-compose.yml` share a default bridge network and can reach each other by their service name. The `order-service` application.yml connects to `jdbc:postgresql://order-db:5432/orderdb` — `order-db` resolves to the PostgreSQL container's IP on the bridge network. This is why service names in `docker-compose.yml` must match the hostnames configured in each service's `application.yml`.

### Health Checks

Production-grade Docker Compose configurations define `healthcheck` entries on each dependency container, and application services declare `depends_on` with `condition: service_healthy`. This prevents the Spring Boot application from starting before PostgreSQL is ready to accept connections — a common source of startup failures in containerised environments.

---

## 4. Architectural Philosophy: Event-Driven Microservices

### Why Microservices?

Microservices decompose a monolithic application into independently deployable modules. The business justification: if `order-service` experiences a 10× traffic spike during a sale, we can deploy five additional instances of `order-service` without touching `auth-service` or `inventory-service`. Each service scales independently based on its own load profile.

The engineering justification: an unhandled exception in `notification-service` cannot crash `order-service`. The failure boundary is the service boundary. In a monolith, a NullPointerException in the email sending code can bring down order creation.

### The Cost of Microservices

This decomposition is not free. The moment two operations live in different services, you lose the atomic transaction that would make them simple in a monolith. Consider:

**Monolith**: `createOrder()` → saves order → reserves inventory → sends email → all in one database transaction. If anything fails, everything rolls back.

**Microservices**: `createOrder()` in `order-service` → publishes event to RabbitMQ → `inventory-service` consumes event and reserves stock → `notification-service` consumes event and sends email. Three separate processes, three separate databases. No shared transaction.

Every pattern in Meridian — the Outbox, idempotency checks, circuit breakers, dead-letter queues — exists to manage this cost.

### Synchronous vs. Asynchronous Communication

**REST APIs (Synchronous)**: Used when the client needs an immediate response and cannot proceed without it. `POST /api/v1/auth/login` must return a JWT synchronously — there is no asynchronous alternative. The client is blocked waiting.

**Message Brokers (Asynchronous)**: Used for inter-service workflows where the publisher does not need the consumer's response to continue. When `order-service` publishes `OrderCreatedEvent`, it does not wait for `inventory-service` to reserve stock. The user gets an immediate `201 Created` response. The inventory reservation happens in the background, decoupled from the HTTP response cycle.

The rule of thumb for choosing: if the calling service needs data from the called service to continue, use REST. If the calling service just needs to notify something happened, use messaging.

---

## 5. The Clean Architecture Paradigm

### The Dependency Rule

Every service in Meridian enforces a strict four-layer package structure. The governing rule is the **Dependency Rule**: source code dependencies can only point inward. Inner layers have zero knowledge of outer layers.

```
presentation  →  application  →  domain
infrastructure  →  application  →  domain
```

`domain` depends on nothing. `application` depends only on `domain`. `presentation` and `infrastructure` depend on `application`. Nothing depends on `presentation` or `infrastructure`.

### Layer Responsibilities

**`domain`** — The business core. Contains pure Java classes representing the business entities: `User`, `Order`, `OrderItem`, `Product`, `OutboxEvent`. These classes contain business logic — `Product.reserveStock(quantity)` throws a domain exception if stock is insufficient. This is intentional: business rules live in the domain, not scattered across service classes. The only concession to pragmatism is the JPA annotations (`@Entity`, `@Column`) on domain classes to avoid a separate persistence model. Strict Clean Architecture would separate these.

**`application`** — The use-case orchestrators: `AuthService.java`, `OrderService.java`, `InventoryService.java`. These classes receive DTOs from the presentation layer, invoke domain logic, and coordinate with infrastructure repositories to persist state. They are the primary `@Transactional` boundary — all database operations within a single use case are wrapped in one transaction. The application layer defines interfaces (`OrderRepository`, `UserRepository`) but does not implement them. Implementation lives in `infrastructure`.

**`infrastructure`** — The concrete implementations and external integrations. `UserRepository` is implemented here as a Spring Data JPA interface. `EventPublisher` is implemented here using `RabbitTemplate`. `SecurityConfig` and `JwtAuthenticationFilter` live here because they are Spring framework concerns, not business concerns. The application layer calls an `OrderRepository` interface — it does not know or care whether that interface is backed by PostgreSQL, MongoDB, or an in-memory map.

**`presentation`** — The HTTP boundary: REST controllers, request DTOs, response mappers. `AuthController.java` receives an HTTP `POST /api/v1/auth/login`, deserializes the JSON body into a `LoginRequest` DTO, calls `AuthService.login()`, and returns a `LoginResponse` DTO as JSON. Controllers contain zero business logic. They translate HTTP into service calls and service results back into HTTP responses.

### Why This Structure Makes Testing Dramatically Easier

The application layer depends on `OrderRepository` as an interface, not on a specific Spring Data JPA implementation. In a unit test, you inject a mock `OrderRepository` that returns whatever data the test requires. No database, no container, no network. The unit test runs in milliseconds and tests the business logic in `OrderService` in complete isolation from PostgreSQL.

---

## 6. Auth Service Deep Dive

### Stateless JWT Authentication

Traditional session-based authentication stores a session ID in server memory. Every request carries that ID, the server looks it up, and finds the associated user. This breaks in microservices: `order-service` cannot read `auth-service`'s memory. Even with a shared session store (Redis), every service call becomes a synchronous network request to validate the session.

JWT solves this with **cryptographic trust**. The auth service signs a token containing the user's identity and roles. Any other service can verify that token using the shared HMAC-SHA256 secret — no network call, no shared state.

### JWT Structure

A JWT has three Base64URL-encoded segments separated by dots: `header.payload.signature`.

**Header**: `{"alg": "HS256", "typ": "JWT"}` — declares the signing algorithm.

**Payload** (claims):

```json
{
  "sub": "user-uuid-here",
  "username": "zawadi@example.com",
  "roles": ["ROLE_USER"],
  "iat": 1700000000,
  "exp": 1700000900
}
```

`iat` is issued-at timestamp. `exp` is the expiry timestamp. The `JwtTokenProvider` reads the configured `jwt.secret` and `jwt.expiration` from `application.yml` to build these.

**Signature**: `HMAC-SHA256(base64(header) + "." + base64(payload), secret)`. This is what makes the token tamper-proof. If an attacker modifies the payload (e.g., changes `"roles": ["ROLE_ADMIN"]`), the signature no longer matches and `JwtTokenProvider.validateToken()` throws an exception.

### Access Token vs. Refresh Token

This is one of the most important security decisions in the system.

**Access Token**: Deliberately short-lived (15 minutes by default). Sent in `Authorization: Bearer <token>` on every API request. Because it expires quickly, a stolen access token has a narrow window of usability. Any service can verify it without a database call.

**Refresh Token**: Long-lived (7 days). Stored in the `refresh_tokens` table in PostgreSQL, linked to a specific user. When the access token expires, the client sends the refresh token to `POST /api/v1/auth/refresh`. The `AuthService` verifies the token exists in the database (not revoked), generates a new access token, rotates the refresh token (issues a new one and invalidates the old), and returns both.

**Why rotation?** _(Status: Planned - A simpler deletion model is currently implemented; advanced reuse-detection is pending)_ If a refresh token is stolen and used by an attacker, when the legitimate user tries to refresh, their original token is already marked used. `AuthService` detects this (a used token being presented again), flags the account as potentially compromised, and revokes all refresh tokens for that user. This is the **refresh token rotation with reuse detection** pattern.

### The Spring Security Filter Chain

Spring Security processes every HTTP request through a pipeline of filters before it reaches any controller.

**`SecurityConfig.java`** declares the filter chain configuration:

- CSRF is disabled. CSRF protection defends against browser-based attacks where a malicious site tricks a logged-in user's browser into making requests. Since the API clients are not browsers submitting form POSTs, CSRF tokens serve no purpose.
- Session management is set to `STATELESS` — Spring will never create an `HttpSession`. All request context lives in the JWT.
- The `/api/v1/auth/**` paths are permitted without authentication. All other paths require a valid JWT.
- `JwtAuthenticationFilter` is inserted before `UsernamePasswordAuthenticationFilter` in the chain.

**`JwtAuthenticationFilter.java`** runs before every request:

1. Reads the `Authorization` header.
2. Extracts the token after the `Bearer ` prefix.
3. Calls `JwtTokenProvider.validateToken()` — checks signature, expiry, and structure.
4. If valid, calls `JwtTokenProvider.getUsernameFromToken()` to extract the subject claim.
5. Calls `CustomUserDetailsService.loadUserByUsername()` to load the full `UserDetails` object from PostgreSQL.
6. Constructs a `UsernamePasswordAuthenticationToken` and injects it into `SecurityContextHolder.getContext().setAuthentication(...)`.

After step 6, every downstream component — controllers, service methods annotated with `@PreAuthorize` — can access the authenticated user via `SecurityContextHolder.getContext().getAuthentication()`.

**`CustomUserDetailsService.java`** exists because Spring Security's filter expects a `UserDetails` object, not your domain `User` entity. It implements `UserDetailsService` and its `loadUserByUsername()` method calls `UserRepository.findByUsername()`, maps the domain `User` to Spring's `UserDetails` interface, and returns it. This is the adapter between your domain model and Spring's security infrastructure.

### BCrypt Password Hashing

When `AuthService.registerUser()` saves a new user, it calls `passwordEncoder.encode(rawPassword)` before persisting. `passwordEncoder` is a `BCryptPasswordEncoder` bean.

BCrypt does three things:

1. **Salting**: Generates a random 128-bit salt and prepends it to the hash output. Two users with the same password produce completely different hashes.
2. **Work factor**: BCrypt deliberately runs slowly — approximately 100ms per hash at the default work factor of 10. This makes brute-force attacks computationally expensive.
3. **Verification**: `passwordEncoder.matches(rawPassword, storedHash)` re-runs the BCrypt algorithm using the salt embedded in `storedHash` and checks if the result matches. The raw password is never stored.

**Why not SHA-256?** SHA-256 is designed to be fast — billions of hashes per second on modern hardware. A leaked password database hashed with SHA-256 can be brute-forced trivially. BCrypt's deliberate slowness is the point.

---

## 7. Order Service Deep Dive

### The Dual-Write Problem

When a user places an order, two things must happen:

1. The `Order` record must be saved to PostgreSQL.
2. An `OrderCreatedEvent` must be published to RabbitMQ so downstream services act on it.

These are two separate I/O operations targeting two separate systems. There is no distributed transaction protocol that reliably spans a relational database and a message broker in a way that is both correct and performant.

**If you write to PostgreSQL first, then publish to RabbitMQ**: The server crashes between the two operations. The order exists in the database (the user was charged), but the inventory service never received the event. Items are not reserved. A data inconsistency exists with no automatic recovery.

**If you publish to RabbitMQ first, then write to PostgreSQL**: The server crashes between the two operations. The inventory service consumed the event and reserved stock, but the order never made it to the database. Items are reserved for an order that does not exist.

Neither ordering is safe. This is the dual-write problem, and it applies to any system where you write to more than one external resource in a single logical operation.

### The Transactional Outbox Pattern

The solution implemented in `OrderService.createOrder()` avoids the dual-write entirely by ensuring both writes go to the **same database** within a **single transaction**.

```
BEGIN TRANSACTION
  INSERT INTO orders (id, user_id, items, total, status) VALUES (...)
  INSERT INTO outbox_events (id, event_type, payload, published) VALUES (..., false)
COMMIT TRANSACTION
```

The `OutboxEvent` entity contains a serialised JSON representation of the `OrderCreatedEvent`. Both writes succeed or both fail — atomically. The `@Transactional` annotation on `createOrder()` ensures this.

The RabbitMQ publish step is completely removed from the HTTP request/response cycle. The user receives a `201 Created` response the moment the database transaction commits — before any message has been sent.

### The Outbox Scheduler

The `OutboxScheduler.java` class runs independently of HTTP requests. It is annotated with Spring's `@Scheduled(fixedDelay = 2000)`, which means a Spring-managed background thread invokes its `processOutboxEvents()` method every two seconds.

The scheduler's algorithm:

1. `SELECT * FROM outbox_events WHERE published = false` — find all unpublished events.
2. For each event: call `EventPublisher.publishEvent(event.getPayload())` to send the JSON to RabbitMQ.
3. On success: update `outbox_events SET published = true WHERE id = ?`.
4. On failure: catch the exception, log it, leave `published = false`. The event remains in the table and will be retried in 2 seconds.

**At-least-once delivery guarantee**: If RabbitMQ is offline, the scheduler retries indefinitely every 2 seconds. Events accumulate in the `outbox_events` table, safely in PostgreSQL. When RabbitMQ recovers, the backlog is processed. No events are lost.

**The tradeoff**: An order is created before its downstream effects are triggered. The user sees a successful order confirmation, but inventory reservation and notification happen asynchronously, potentially seconds later. This is eventual consistency. For an e-commerce platform, this is acceptable — the order is committed, and the rest will follow. For a payment processor, stricter guarantees would be required.

### RabbitMQ Core Concepts

Understanding the RabbitMQ model in `RabbitMQConfig.java` requires understanding three primitives: exchanges, queues, and bindings.

**Exchange**: The entry point for published messages. Producers publish to an exchange, never directly to a queue. The exchange's job is to route messages to zero or more queues. `orders.exchange` is declared as a **topic exchange**.

**Queue**: A buffer where messages wait to be consumed. `order.created.queue` holds `OrderCreatedEvent` messages until a consumer is ready to process them. Queues are `durable: true` — they survive a RabbitMQ broker restart.

**Binding**: A routing rule between an exchange and a queue. A binding with routing key `order.created.key` means: "when a message arrives at `orders.exchange` with routing key `order.created.key`, deliver it to `order.created.queue`." _(Status: Implemented - Note the actual routing key is `order.created.key`, not exactly `order.created` as implied in the topic wildcard example below)_

**Topic exchange routing keys**: Topic exchanges support wildcard routing keys. `order.*` matches `order.created` and `order.cancelled`. `order.#` matches `order.created`, `order.cancelled`, and `order.items.updated`. This is why topic exchanges are used: they allow future routing rules to be added without changing the exchange or the existing queues.

**Dead Letter Queue (DLQ)**: When a consumer throws an exception processing a message and the retry limit is exceeded, RabbitMQ routes the message to the DLQ instead of discarding it. `order.created.queue.dlq` is the dead-letter queue for `order.created.queue`. Messages in the DLQ can be inspected, replayed, or handled by a separate DLQ consumer — nothing is silently lost.

### Domain Events and the `DomainEvent` Hierarchy

`OrderCreatedEvent` extends a base `DomainEvent` class. This hierarchy exists to support a generic `OutboxEvent` entity: the outbox stores any `DomainEvent` subclass as a serialised JSON string in the `payload` column, along with an `event_type` discriminator string (e.g., `"ORDER_CREATED"`). When the scheduler publishes the event, it uses the `event_type` to determine which RabbitMQ routing key to use. This allows the outbox mechanism to be extended to new event types without modifying the scheduler logic.

---

## 8. API Gateway Deep Dive

### Why a Gateway?

Without a gateway, every client must know the network address and port of every service. A mobile client creates an order by calling `order-service:8082` directly. This exposes your internal service topology to the internet and means every cross-cutting concern — authentication, rate limiting, logging — must be implemented in every service independently.

The gateway is a **single entry point** that:

- Routes requests to the correct downstream service based on path patterns.
- Validates JWTs once, before any downstream service is involved.
- Enforces rate limits.
- Applies circuit breakers to protect services from cascade failures.
- Handles TLS termination (in production).

Every external request goes through the gateway. Downstream services are never directly accessible from outside the Docker network.

### Spring Cloud Gateway and Reactive Architecture

This is the most technically subtle architectural detail in the project. The gateway is built on **Spring WebFlux**, the reactive (non-blocking) web framework, rather than **Spring MVC** (the servlet-based, blocking framework). The two are mutually exclusive on the classpath — you cannot mix them in the same application.

**Blocking I/O (Spring MVC)**: Each HTTP request is handled by a dedicated thread from a thread pool. If the downstream service takes 200ms to respond, that thread is blocked and unavailable for other requests. With a pool of 200 threads, 200 concurrent slow requests exhaust the pool and new requests are queued.

**Non-blocking I/O (WebFlux)**: A small number of event-loop threads handle all I/O. When a request is forwarded to `order-service` and the gateway is waiting for the response, the event loop thread is released to handle other incoming requests. The continuation (sending the response back to the client) is registered as a callback. A gateway with 4 event-loop threads can handle thousands of concurrent in-flight requests.

For a gateway whose primary job is I/O (receiving a request, forwarding it, receiving the downstream response, forwarding it back), non-blocking is dramatically more efficient than blocking. This is why Spring Cloud Gateway requires WebFlux.

### Route Configuration

Each route in `application.yml` has three parts:

**Predicates** define when a route matches. `Path=/api/v1/auth/**` matches any URL starting with that prefix. Predicates can be composed: a route could require both a specific path AND a specific HTTP method.

**Filters** transform the request or response. `StripPrefix=1` removes the first path segment before forwarding — a request to `/api/v1/auth/login` arrives at the downstream `auth-service` as `/api/v1/auth/login` (the gateway does not add a prefix, it preserves the path after stripping the configured count of path segments from the front).

**URI** is the downstream target. `http://auth-service:8081` uses the Docker Compose service name `auth-service` as a hostname — Docker's internal DNS resolves it to the container's IP on the bridge network.

### Rate Limiting with Redis Token Bucket

The `RequestRateLimiter` filter implements a **token bucket** algorithm backed by Redis.

Conceptually, every requesting IP address has a bucket. The bucket starts full. Each request consumes one token. The bucket refills at a constant `replenish-rate` (tokens per second). `burst-capacity` is the maximum number of tokens the bucket can hold — it controls how many requests can arrive simultaneously before the rate limit kicks in.

When a request arrives:

1. A Lua script executes atomically in Redis (Lua scripts in Redis are atomic — no other operation can interleave).
2. The script reads the current token count and the last refill timestamp for this IP.
3. It calculates how many tokens have replenished since the last request.
4. If at least one token is available, it decrements the count and allows the request through.
5. If the bucket is empty, it returns a `429 Too Many Requests` response without touching the downstream service.

**Why Redis and not in-memory?** If the gateway runs as three instances behind a load balancer (for high availability), each instance has its own memory. An attacker sending 100 requests/second distributed across three instances would bypass an in-memory limit of 50 requests/second per instance. Redis is shared — all instances decrement the same counter.

### Circuit Breakers with Resilience4j

A circuit breaker wraps a call to a downstream service and monitors its health. It operates in three states:

**CLOSED** (healthy): All requests pass through. The breaker counts failures. `slidingWindowType: COUNT_BASED` with `slidingWindowSize: 10` means it evaluates the last 10 requests. If the `failureRateThreshold` (e.g., 50%) is exceeded — 5 out of 10 requests fail — the breaker trips to OPEN.

**OPEN** (unhealthy): The downstream service is considered unavailable. The breaker immediately rejects all requests for `waitDurationInOpenState` (e.g., 10 seconds). It does not attempt to call the service. Instead, it calls the configured `fallbackUri` — a controller method in `FallbackController.java` that returns a structured error response. This is **graceful degradation**: users get a meaningful "service temporarily unavailable" message rather than a connection timeout.

**HALF-OPEN** (probing): After the cooldown period, the breaker allows `permittedNumberOfCallsInHalfOpenState` test requests through. If they succeed, it returns to CLOSED. If they fail, it returns to OPEN and the cooldown resets.

Without a circuit breaker, a slow or unavailable `order-service` causes gateway threads to block waiting for connections that never respond. With Tomcat's bounded thread pool, this exhausts the gateway under load — the entire gateway becomes unresponsive because of one downstream failure. The circuit breaker prevents this cascade.

### Reactive Security — `ServerHttpSecurity`

Because the gateway uses WebFlux, Spring Security's standard `HttpSecurity` (designed for servlet-based applications) is replaced by `ServerHttpSecurity`. The JWT validation is implemented using Spring's reactive OAuth2 Resource Server support: `http.oauth2ResourceServer(oauth2 -> oauth2.jwt(jwt -> ...))`. The configured JWT decoder verifies the HMAC-SHA256 signature against the same shared secret used by `auth-service` to issue tokens.

---

## 9. Inventory Service Deep Dive

### Why MongoDB for the Product Catalogue?

Products have heterogeneous attributes. A T-shirt has `size` (`S/M/L/XL`) and `colour`. A laptop has `ramGB`, `storageGB`, and `screenResolutionInches`. A book has `isbn`, `author`, and `pageCount`.

In PostgreSQL, representing this would require either:

- A single `products` table with hundreds of nullable columns (most NULL for any given product type). This is called the "wide table" anti-pattern.
- An Entity-Attribute-Value (EAV) model with a `product_attributes` table storing `(product_id, attribute_name, attribute_value)` as strings. EAV makes queries complex and type safety impossible.

MongoDB stores each product as a self-contained JSON document. A T-shirt document has `size` and `colour` fields. A laptop document has `ramGB` and `storageGB` fields. The schema is defined by the product's category, not by a rigid table schema. New product categories can be added without schema migrations.

### Document Modelling Details

**`Product` document**: The `@Document(collection = "products")` annotation maps the class to the MongoDB `products` collection. Key annotations:

- `@Id` on the `id` field maps to MongoDB's `_id` field (stored as `ObjectId` by default).
- `@Indexed(unique = true)` on `sku` creates a unique index at the MongoDB level. If two documents attempt to use the same SKU, MongoDB throws a `DuplicateKeyException`. Business key uniqueness is enforced at the database, not just at the application layer.
- `@Version` on a `Long version` field enables **optimistic locking**. When a document is read, its current version is recorded. When it is saved, MongoDB checks that the version in the database still matches. If another process modified the document between read and save (incremented the version), MongoDB rejects the update with an `OptimisticLockingFailureException`. The application catches this and retries. This prevents lost updates when two consumers attempt to reserve stock from the same product simultaneously.
- `@CreatedDate` and `@LastModifiedDate` on timestamp fields are populated automatically by Spring Data's auditing infrastructure — the class must be annotated with `@EnableMongoAuditing` in the configuration class.

**`InventoryReservation` document**: A separate document linking `orderId` → `productId` with a `quantity` and `ReservationStatus` enum (`PENDING`, `CONFIRMED`, `RELEASED`). Keeping reservations as separate documents creates an immutable audit trail. The question "which orders have reserved units of product X?" is answered by querying this collection — no modification to the product document is needed.

### The Consumer Pattern — Receiving Events

`OrderCreatedConsumer.java` is the receiving end of the event chain initiated by `order-service`'s Outbox Pattern.

The `@RabbitListener(queues = "order.created.queue")` annotation on a method instructs Spring AMQP to register this method as a consumer for that queue. When a message arrives, Spring AMQP:

1. Reads the raw bytes from RabbitMQ.
2. Uses `Jackson2JsonMessageConverter` to deserialise the JSON payload into the `OrderCreatedEvent` DTO. This converter must be declared as a Spring bean in `RabbitMQConfig.java` — without it, Spring AMQP uses a default converter that cannot deserialise JSON into POJOs.
3. Invokes the annotated method with the deserialised object.

If the method completes without throwing an exception, Spring AMQP sends an `ACK` to RabbitMQ, which removes the message from the queue. If the method throws an exception, Spring AMQP sends a `NACK`, and RabbitMQ redelivers the message (up to the configured retry limit, after which it routes to the DLQ).

### Idempotency — Handling At-Least-Once Delivery

RabbitMQ guarantees **at-least-once delivery** — it will try hard to deliver every message at least once, but under some failure conditions (consumer crashes after processing but before acknowledging), the message may be delivered a second time.

`InventoryService.reserveStock()` handles this by checking for existing reservations before processing:

```java
if (reservationRepository.existsByOrderId(orderId)) {
    return; // Already processed — silently skip
}
```

This is the **idempotency check**. If the same `OrderCreatedEvent` is delivered twice (same `orderId`), the second delivery finds an existing reservation and returns without deducting stock a second time. The operation is idempotent: applying it multiple times has the same effect as applying it once.

Without this check, a network partition causing a message redelivery would result in double-reservation — the customer charged once but inventory debited twice.

### Domain Logic in the Entity — `Product.reserveStock()`

The business rule "you cannot reserve more units than are available" is enforced in a method on the `Product` entity itself, not in `InventoryService`:

```java
public void reserveStock(int quantity) {
    if (this.availableQuantity < quantity) {
        throw new InsufficientStockException(this.sku, quantity, this.availableQuantity);
    }
    this.availableQuantity -= quantity;
}
```

This is the **domain logic in the entity** principle from Clean Architecture. The invariant belongs to the `Product` aggregate. If this check lived in `InventoryService`, another service method could directly decrement `availableQuantity` without the check — the invariant would be unenforceable. By making `availableQuantity` writable only through `reserveStock()`, the domain protects its own consistency.

---

## 10. Notification Service Deep Dive

_(To be written as the notification-service module is constructed.)_

### Planned: DLQ Consumer Architecture

The notification service will consume from two queues:

- `order.created.queue` for order confirmation emails.
- `order.created.queue.dlq` to handle and alert on failed messages from the order queue.

### Planned: Email Sending with JavaMailSender

Spring Boot's `spring-boot-starter-mail` provides `JavaMailSender`. Configuration in `application.yml` specifies the SMTP host, port, and credentials. The service will use Thymeleaf templates for HTML email rendering.

### Planned: Retry and Fallback Logic

If the SMTP server is unavailable, the message will not be ACKed, allowing RabbitMQ to redeliver. After a configured number of failures, the message routes to the notification DLQ for manual inspection.

---

## 11. Analytics Service Deep Dive

The Analytics Service is responsible for consuming domain events (like `OrderCreatedEvent` and `InventoryReservedEvent`), persisting them as time-series metrics, and exposing aggregated data via a GraphQL API. It strictly adheres to Clean Architecture, containing pure `OrderMetric` and `InventoryMetric` POJOs in its domain layer, port interfaces in its application layer, and framework bindings (InfluxDB, GraphQL, RabbitMQ) in the infrastructure layer.

### [Status: Implemented] GraphQL API with Spring for GraphQL

The analytics service exposes a GraphQL API rather than REST. GraphQL is appropriate here because analytics queries are inherently variable — clients need flexible querying over metrics without requiring a new REST endpoint for every combination.

Spring Boot 3.x includes first-class support for GraphQL via `spring-boot-starter-graphql`. Schema-first approach: define the GraphQL schema in `schema.graphqls` files, then implement resolver methods annotated with `@QueryMapping` (e.g., `OrderMetricsResolver`, `InventoryMetricsResolver`). Notably, these resolvers act as the "presentation" boundary, replacing traditional REST controllers under the infrastructure namespace.

**The N+1 problem in GraphQL** is solved by `@BatchMapping` — Spring for GraphQL's equivalent of the DataLoader pattern. When a query requests a list of orders each with their analytics, `@BatchMapping` batches all analytics lookups into a single query rather than N individual queries.

### [Status: Implemented] InfluxDB for Time-Series Data

InfluxDB is a purpose-built time-series database. A relational database can store time-series data, but it requires careful index design and partitioning to remain performant at scale. InfluxDB stores data in measurements (analogous to tables) organised by time, with tags (indexed metadata) and fields (unindexed values).

**Implementation Detail**: Because Spring Boot 3.x does not provide an official `spring-boot-starter-influxdb` that supports InfluxDB 2.x natively, the service integrates the official `influxdb-client-java` dependency directly. The `InfluxDBMetricsWriter` uses the `WriteApiBlocking` to ingest data points, while `InfluxDBMetricsReader` executes raw `Flux` queries to aggregate and retrieve time-series metrics. Queries like "average order value" execute natively using `|> mean()` without complex SQL window functions.

---

## 12. Observability — Micrometer, Prometheus, and Jaeger

### The Three Pillars of Observability

Observability answers the question "why is this system behaving this way right now?" It has three independent axes:

**Logs** (events): Discrete records of what happened. "User 42 placed order 99 at 14:32:01." Logs are useful for debugging specific incidents but difficult to aggregate quantitatively.

**Metrics** (measurements): Numeric aggregates over time. "The p99 latency of `POST /api/v1/orders` over the last 5 minutes was 340ms." "2.3 orders per second are being processed." Metrics are efficiently queryable but lose individual event detail.

**Traces** (request flows): A record of a single request's journey across multiple services. A trace for `POST /api/v1/orders` shows the time spent in the gateway, in `order-service`, and the time until the RabbitMQ message was published. Traces reveal exactly where latency lives in a distributed system.

You need all three. Metrics tell you _something is wrong_. Logs tell you _what happened_. Traces tell you _where the slowness is_.

### Micrometer — The Metrics Abstraction Layer

Micrometer is to metrics what SLF4J is to logging — a vendor-neutral API that abstracts the underlying metrics backend. Your application code uses `MeterRegistry` to record metrics. At runtime, the `MeterRegistry` is bound to Prometheus (or Datadog, or InfluxDB) via a backend-specific library.

`spring-boot-starter-actuator` auto-configures Micrometer and exposes a `/actuator/metrics` endpoint. Adding `micrometer-registry-prometheus` to the POM automatically formats metrics in Prometheus scrape format at `/actuator/prometheus`.

**Custom business metrics** are declared in service classes:

```java
@Autowired
private MeterRegistry meterRegistry;

// In createOrder():
meterRegistry.counter("orders.created", "status", "success").increment();
meterRegistry.timer("orders.creation.duration").record(duration, TimeUnit.MILLISECONDS);
```

This produces two metrics: a counter `orders.created{status="success"}` and a timer `orders.creation.duration` with automatically computed percentile histograms.

### Prometheus — The Metrics Store

Prometheus is a time-series database that **scrapes** metrics from instrumented services at a configured interval (e.g., every 15 seconds). It pulls metrics from `http://order-service:8082/actuator/prometheus`. This is the opposite of push-based metrics systems — services do not need to know where Prometheus lives.

Prometheus stores the scraped data as time-series, indexed by metric name and label combinations. PromQL (Prometheus Query Language) queries this data: `rate(orders_created_total[5m])` computes the per-second order creation rate over a 5-minute window.

### Grafana — Metrics Visualisation

Grafana connects to Prometheus as a data source and provides dashboards. The `docker-compose.yml` defines both a Prometheus container and a Grafana container. A Grafana dashboard can display the p99 latency of every endpoint, RabbitMQ queue depths, JVM heap usage, and PostgreSQL connection pool utilisation — all from the Prometheus time-series data.

### OpenTelemetry and Jaeger — Distributed Tracing

OpenTelemetry is the vendor-neutral standard for distributed tracing instrumentation. When `spring-boot-starter-actuator` and the OpenTelemetry exporter are on the classpath, Spring Boot auto-instruments HTTP requests, database queries, and messaging operations — each becomes a **span** in a **trace**.

A trace for `POST /api/v1/orders`:

```
Trace: 8f3a-9b2c-...
  Span: api-gateway HTTP request (5ms)
    Span: order-service HTTP handler (45ms)
      Span: PostgreSQL INSERT orders (12ms)
      Span: PostgreSQL INSERT outbox_events (3ms)
      Span: RabbitMQ publish (2ms)
```

The `traceId` is propagated between services via HTTP headers (`traceparent` in the W3C standard). Jaeger receives these spans from all services and assembles them into the complete trace visualisation.

In `application.yml`, every service is configured to export trace data to Jaeger:

```yaml
management:
  tracing:
    sampling:
      probability: 1.0 # Sample 100% of requests in development
  otlp:
    tracing:
      endpoint: http://jaeger:4318/v1/traces
```

In production, sampling at 100% is too expensive. Typical production sampling rates are 1–10%, with head-based or tail-based sampling strategies to ensure errors are always captured regardless of the sample rate.

---

## 13. Testing Strategy

### The Test Pyramid

The test pyramid describes the optimal distribution of test types: many unit tests at the base, fewer integration tests in the middle, and minimal end-to-end tests at the top. Unit tests are fast, cheap, and deterministic. End-to-end tests are slow, expensive, and brittle.

Meridian's testing approach follows this structure.

### Unit Tests — Service Layer

Unit tests in `auth-service` and `order-service` test the application service classes in isolation. No Spring context, no database, no RabbitMQ.

**Mocking with Mockito**: `@Mock` creates a mock of `UserRepository`. `@InjectMocks` creates an instance of `AuthService` with the mock injected. `when(userRepository.findByUsername("test")).thenReturn(Optional.of(user))` configures the mock's behaviour. The test then calls `authService.login(request)` and asserts on the returned token.

**What to test in unit tests**:

- Business logic branches (successful registration, duplicate email rejection, invalid password handling).
- Transaction boundaries (does `createOrder` call both `orderRepository.save()` and `outboxRepository.save()`?).
- Exception handling (does `OrderService.createOrder()` throw `OrderNotFoundException` when the product does not exist?).

**What NOT to test in unit tests**: SQL query correctness, RabbitMQ message delivery, HTTP request/response mapping. These belong in integration tests.

### Integration Tests — Repository and API Layer

Integration tests use Spring Boot Test's `@SpringBootTest` to start a real (or partial) Spring application context. `@DataJpaTest` starts only the JPA layer against an H2 in-memory database to test repository queries.

`WebApplicationFactory` with `MockMvc` or `WebTestClient` tests the full HTTP stack:

```java
mockMvc.perform(post("/api/v1/auth/login")
    .contentType(MediaType.APPLICATION_JSON)
    .content("{\"username\": \"test\", \"password\": \"password\"}"))
    .andExpect(status().isOk())
    .andExpect(jsonPath("$.accessToken").isNotEmpty());
```

This starts the full Spring Boot application with an in-memory database, makes a real HTTP request through the servlet filters (including `JwtAuthenticationFilter`), and asserts on the JSON response. It tests the entire vertical slice from HTTP to database without a real network.

### Testing the Outbox Pattern

Testing `OrderService.createOrder()` requires verifying that both the `Order` and the `OutboxEvent` are saved. With a mocked `OrderRepository` and `OutboxRepository`:

```java
// Assert both saves were called within the same logical operation
verify(orderRepository, times(1)).save(any(Order.class));
verify(outboxRepository, times(1)).save(any(OutboxEvent.class));
// Assert RabbitMQ was NOT called directly from createOrder()
verify(eventPublisher, never()).publishEvent(any());
```

This confirms the transactional boundary is correct — RabbitMQ publishing is deliberately absent from `createOrder()`.

---

## 14. Security Architecture — Cross-Cutting Concerns

### Shared JWT Secret

All services that validate JWTs (the gateway, `order-service`, `inventory-service`) share the same HMAC-SHA256 secret. In development this is a string in `application.yml`. In production, this secret must be loaded from a secrets manager (Azure Key Vault, AWS Secrets Manager, HashiCorp Vault) — never committed to version control.

The `jwt.secret` property should be at least 256 bits (32 bytes) of cryptographically random data, base64-encoded. A predictable or short secret is vulnerable to brute-force attacks.

### Defence in Depth

Each layer enforces its own security independently:

- **Gateway**: Validates JWT signature and expiry. Rejects requests with invalid tokens before they reach downstream services.
- **Downstream services**: Also validate the JWT (via Spring Security's resource server configuration). Services do not trust that the gateway has already validated — they validate independently. This means even if the gateway is bypassed (e.g., a developer accesses `order-service` directly in a staging environment), the security holds.
- **Database layer**: PostgreSQL Row-Level Security (when enabled) restricts which rows a database user can access. The application's database user does not have `DROP TABLE` or `CREATE TABLE` privileges in production.

### CORS in a Microservices Architecture

CORS is only relevant where browser-based JavaScript clients make HTTP requests to API endpoints. The gateway is the single entry point and the only service exposed to browser clients. CORS should be configured exclusively in the gateway — not in each downstream service. Downstream services are never directly accessible from browsers.

---

## 15. Data Architecture — Polyglot Persistence

Meridian intentionally uses different databases for different services. This is called **polyglot persistence** — choosing the best data store for each use case rather than forcing all data into a single database type.

| Service             | Database   | Justification                                                                                                                                                                             |
| ------------------- | ---------- | ----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| `auth-service`      | PostgreSQL | Users and refresh tokens are relational data with strict consistency requirements. ACID transactions are essential.                                                                       |
| `order-service`     | PostgreSQL | Orders, line items, and outbox events require ACID transactions across multiple tables in a single atomic commit.                                                                         |
| `inventory-service` | MongoDB    | Product catalogue has heterogeneous schemas. Document model avoids EAV anti-pattern.                                                                                                      |
| `api-gateway`       | Redis      | Rate limit counters are ephemeral, high-frequency writes. Redis is orders of magnitude faster than PostgreSQL for this workload.                                                          |
| `analytics-service` | InfluxDB   | Time-series data (metrics over time) is the primary query pattern. Purpose-built time-series databases outperform relational databases for this workload by multiple orders of magnitude. |

### Why Not One Big PostgreSQL?

You could store everything in PostgreSQL. Product attributes in a JSONB column. Rate limit counters in a table with expiry timestamps (using pg_cron to clean them up). Time-series metrics in a partitioned table with a time index.

PostgreSQL can do all of this. The tradeoffs: PostgreSQL is not as fast as Redis for high-frequency counter increments. PostgreSQL does not offer InfluxDB's native time-series query operators. MongoDB's document model is more natural for heterogeneous schemas than JSONB in a shared table.

More importantly, separate databases reinforce the service isolation principle. If `inventory-service` and `order-service` share a PostgreSQL instance, a slow `VACUUM` operation triggered by order service writes can degrade inventory queries. Shared infrastructure creates shared failure modes.

---

## 16. Summary of Completed Code Modules

### Auth Service

- **Domain**: `User` (entity, BCrypt-hashed password field), `Role` (enum: `ROLE_USER`, `ROLE_ADMIN`), `RefreshToken` (entity, one-use semantics with reuse detection).
- **Application**: `AuthService` (`registerUser`, `login`, `refreshToken`, `logout`). Primary `@Transactional` boundary.
- **Infrastructure**: `UserRepository`, `RefreshTokenRepository` (Spring Data JPA). `JwtTokenProvider` (sign, validate, extract claims). `JwtAuthenticationFilter` (per-request validation). `SecurityConfig` (filter chain, CSRF disabled, stateless sessions, path whitelist). `CustomUserDetailsService` (domain User → Spring UserDetails adapter). `BCryptPasswordEncoder` bean.
- **Presentation**: `AuthController` (`/register`, `/login`, `/refresh`). `RegisterRequest`, `LoginRequest`, `LoginResponse`, `RefreshRequest` DTOs.
- **Tests**: Unit tests for `AuthService` (registration success, duplicate email, wrong password). Integration tests for `/login` and `/register` endpoints.

### Order Service

- **Domain**: `Order` (entity, status lifecycle), `OrderItem` (embedded entity, product SKU + quantity + price), `OutboxEvent` (entity, JSON payload, `published` flag), `DomainEvent` (abstract base class for event hierarchy), `OrderCreatedEvent` (concrete event DTO).
- **Application**: `OrderService` (`createOrder` with `@Transactional` outbox write). `OrderMapper` (domain ↔ DTO mapping).
- **Infrastructure**: `OrderRepository`, `OutboxEventRepository` (Spring Data JPA). `EventPublisher` (wraps `RabbitTemplate`). `RabbitMQConfig` (exchange, queue, binding, DLQ declarations, `Jackson2JsonMessageConverter`). `OutboxScheduler` (`@Scheduled`, polls unpublished events, publishes to RabbitMQ).
- **Presentation**: `OrderController` (`POST /api/v1/orders`, `GET /api/v1/orders`, `GET /api/v1/orders/{id}`). `CreateOrderRequest`, `OrderResponse` DTOs.
- **Tests**: Unit tests for `OrderService` (outbox write verification, publisher not called directly).

### API Gateway

- **Infrastructure**: `application.yml` route definitions (predicates, `StripPrefix` filter, downstream URIs). `SecurityConfig` (reactive `ServerHttpSecurity`, OAuth2 resource server JWT validation). `RateLimiterConfig` (Redis token bucket, `KeyResolver` by IP). `CircuitBreakerConfig` (Resilience4j configuration). `FallbackController` (graceful degradation responses).
- **Core Flow**: Request → JWT validation (rejected if invalid, returns 401) → rate limit check (rejected if bucket empty, returns 429) → circuit breaker check (rejected if circuit open, returns 503 via fallback) → proxy to downstream service → response forwarded to client.

### Inventory Service

- **Domain**: `Product` (MongoDB document, optimistic locking with `@Version`, `@Indexed(unique=true)` on SKU, `reserveStock()` domain method). `InventoryReservation` (MongoDB document, orderId-to-productId audit trail). `ReservationStatus` (enum).
- **Infrastructure**: `ProductRepository`, `InventoryReservationRepository` (Spring Data MongoDB). `RabbitMQConfig` (consumer queue binding, `Jackson2JsonMessageConverter`). `OrderCreatedConsumer` (`@RabbitListener`, idempotency check, delegates to `InventoryService` - _Status: Currently placed in the application.event package as an application-layer adapter_). `SecurityConfig` (JWT validation as resource server).
- **Core Flow**: `OrderCreatedEvent` arrives via RabbitMQ → `OrderCreatedConsumer` checks idempotency → delegates to `InventoryService.reserveStock()` → per-SKU `product.reserveStock(quantity)` (domain invariant enforced) → `InventoryReservation` persisted to MongoDB → ACK sent to RabbitMQ.

---

_This document is updated concurrently with the codebase. Every new module, design decision, or pattern change is documented here before the module is marked complete._

---

## Documentation Standard

This document is written to a specific standard. Every contributor must understand and maintain it.

**Depth requirement**: Every concept is explained at three levels — _what it is_ (one sentence), _how it works_ (mechanical detail), and _why it exists_ (the problem it solves and the alternatives rejected). A section that only explains _what_ without _why_ is incomplete. The _why_ is the part that transfers to the next project, the interview room, and the architectural discussion with a senior engineer.

**Code references**: Class names, method names, and annotation names are referenced precisely as they appear in the codebase. When code is quoted, it must match the actual implementation. If the implementation changes, the documentation changes in the same commit.

**Failure mode coverage**: Every pattern documented here must explain at least one specific failure scenario it addresses. The happy path is obvious. The failure path is where engineering judgment lives.

**No assumed knowledge**: Concepts from external frameworks (Spring Security, RabbitMQ, MongoDB) are explained in context, not assumed. The reader may have read the official documentation, but this document explains _how those frameworks are applied in this specific architecture_ and _why this project's usage differs from the default tutorial approach_.

**Update discipline**: This document is a living record, not a one-time writeup. When a service is refactored, when a pattern is replaced, when a new service is added — this document is updated in the same pull request. A codebase with an outdated learning document is worse than one with no learning document, because the outdated version actively misleads.

**Target reader**: A senior backend engineer reviewing this project for the first time, or the author returning to this project after six months. Both should be able to understand every architectural decision without asking questions.
