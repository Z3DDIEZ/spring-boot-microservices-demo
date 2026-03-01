# Meridian Backend

Meridian is a production-grade, event-driven microservices platform demonstrating distributed systems architecture, asynchronous messaging, polyglot persistence, and enterprise security patterns. Built with Spring Boot and designed to showcase backend engineering competency for tech markets.

**Core Value Proposition**: Demonstrates mastery of microservices orchestration, message-driven architecture, OAuth2 security, and distributed observability -- the exact patterns tested in senior backend interviews.

## Technology Stack

| Layer            | Technology                                    |
| ---------------- | --------------------------------------------- |
| Runtime          | Java 21 LTS, Spring Boot 3.5.x                |
| Build            | Maven (multi-module reactor)                  |
| Auth             | Spring Security, JWT (access + refresh)       |
| Messaging        | RabbitMQ 4.x (Topic Exchange, Outbox)         |
| Data (SQL)       | PostgreSQL 18.x, Spring Data JPA              |
| Data (NoSQL)     | MongoDB 8.0.x (planned)                       |
| Observability    | Micrometer, Prometheus, Jaeger, OpenTelemetry |
| Containerisation | Docker Compose                                |

## Current Progress

- [x] **Auth Service** -- Spring Security, JWT (access + refresh tokens), BCrypt, PostgreSQL, unit tests
- [x] **Order Service** -- REST API, Transactional Outbox Pattern, RabbitMQ producer, domain events, unit tests
- [ ] **API Gateway** -- Spring Cloud Gateway, JWT validation filter, rate limiting
- [ ] **Inventory Service** -- MongoDB, event consumer, stock reservations
- [ ] **Notification Service** -- Email notifications, DLQ handling
- [ ] **Analytics Service** -- GraphQL, InfluxDB, time-series metrics

## Project Structure

```
meridian-backend/
  pom.xml                  # Parent POM (dependency management)
  docker-compose.yml       # Infrastructure containers
  auth-service/            # OAuth2 authorization server
  order-service/           # Order management + outbox events
  api-gateway/             # (planned) Single entry point
  inventory-service/       # (planned) Product catalog
  notification-service/    # (planned) Async email
  analytics-service/       # (planned) GraphQL metrics
  shared-lib/              # (planned) Shared types
  docs/                    # Architecture documentation
  AG-docs/                 # Internal planning artifacts
```

## Architecture Highlights

- **Clean Architecture**: Each service enforces Domain -> Application -> Infrastructure -> Presentation layering with strict dependency rules.
- **Transactional Outbox Pattern**: Order creation and domain event persistence happen in a single `@Transactional` boundary, guaranteeing at-least-once delivery even if RabbitMQ is temporarily unavailable.
- **Stateless JWT Security**: Access tokens are short-lived; refresh tokens are rotated in PostgreSQL with one-use semantics.
- **Event-Driven Communication**: Services communicate asynchronously via RabbitMQ topic exchanges. No synchronous inter-service HTTP calls.

## API Endpoints

### Auth Service (port 8081)

| Method | Endpoint                | Description           | Auth   |
| ------ | ----------------------- | --------------------- | ------ |
| POST   | `/api/v1/auth/register` | Register new user     | Public |
| POST   | `/api/v1/auth/login`    | Login, receive tokens | Public |
| POST   | `/api/v1/auth/refresh`  | Refresh access token  | Public |

### Order Service (port 8082)

| Method | Endpoint              | Description                      | Auth |
| ------ | --------------------- | -------------------------------- | ---- |
| POST   | `/api/v1/orders`      | Create a new order               | JWT  |
| GET    | `/api/v1/orders`      | List authenticated user's orders | JWT  |
| GET    | `/api/v1/orders/{id}` | Get order by ID                  | JWT  |

## Running Locally

**Prerequisites**: Java 21, Maven 3.9+, Docker (for infrastructure containers)

```bash
# Start infrastructure (PostgreSQL, RabbitMQ)
docker compose up -d auth-db order-db rabbitmq

# Compile all active modules
mvn compile

# Run tests
mvn test -pl auth-service
mvn test -pl order-service
```

## Documentation

- [Architecture Overview](docs/architecture-overview.md)
- [Technology Stack](docs/technology-stack.md)
- [API Contracts](docs/api-contracts.md)
- [Event-Driven Patterns](docs/event-driven-patterns.md)
- [Observability Guide](docs/observability-guide.md)

## License

[MIT](LICENSE)
