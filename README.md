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
| Containerisation | Docker, Kubernetes (K3s/Minikube)             |
| CI/CD & Deploy   | GitHub Actions, Helm                          |

## Current Progress

- [x] **Auth Service** -- Spring Security, JWT (access + refresh tokens), BCrypt, PostgreSQL, unit tests
- [x] **Order Service** -- REST API, Transactional Outbox Pattern, RabbitMQ producer, domain events, unit tests
- [x] **API Gateway** -- Spring Cloud Gateway, JWT validation filter, rate limiting, circuit breakers
- [x] **Inventory Service** -- MongoDB, event consumer, stock reservations, validation
- [x] **Notification Service** -- Email notifications, DLQ handling
- [x] **Analytics Service** -- GraphQL, InfluxDB, time-series metrics
- [x] **Shared Library** -- Centralized domain events and DTOs to enforce schema consistency

## Project Structure

```
meridian-backend/
  pom.xml                  # Parent POM (dependency management)
  docker-compose.yml       # Infrastructure containers
  auth-service/            # OAuth2 authorization server
  order-service/           # Order management + outbox events
  api-gateway/             # Gateway with Resilience4j & Redis rate limiting
  inventory-service/       # Product catalog & stock reservation
  notification-service/    # Async email
  analytics-service/       # GraphQL metrics
  shared-lib/              # Shared types and domain events
  docs/                    # Architecture documentation
  AG-docs/                 # Internal planning artifacts
```

## Architecture Highlights

- **Clean Architecture**: Each service enforces Domain -> Application -> Infrastructure -> Presentation layering with strict dependency rules.
- **Centralized Shared Data Types (`shared-lib`)**: Initially, domain events (like `OrderCreatedEvent`) were duplicated across services during early scaffolding—a lapse in judgment that contradicted the initial design blueprints. This has been corrected by extracting a `shared-lib` module. This ensures strict schema consistency between publishers and consumers without duplicate classes, eliminating deserialization mismatches.
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

### Inventory Service (port 8083)

| Method | Endpoint                | Description                 | Auth   |
| ------ | ----------------------- | --------------------------- | ------ |
| POST   | `/api/v1/products`      | Create a new product        | ADMIN  |
| GET    | `/api/v1/products`      | List all available products | Public |
| GET    | `/api/v1/products/{id}` | Get product by ID           | Public |

## Running Locally

**Prerequisites**: Java 21, Maven 3.9+, Docker, Kubernetes (Docker Desktop/Minikube/K3s), Helm 3

### Option A: Docker Compose (Infrastructure Only)

Useful for running just the databases and building the apps in your IDE:

```bash
# Start infrastructure (PostgreSQL, RabbitMQ, etc.)
docker compose up -d

# Compile all active modules
mvn compile
```

### Option B: Full Kubernetes Deployment (Helm)

Deploys the entire microservice ecosystem and infrastructure into a local K8s cluster. Note: requires at least 8GB+ RAM allocated to the Docker Engine.

```bash
# Deploy all services and infrastructure
helm upgrade --install meridian-backend ./helm/meridian-backend \
  --values ./helm/meridian-backend/values.dev.yaml \
  --namespace meridian --create-namespace

# Watch pods spin up
kubectl get pods -n meridian -w

# Test Gateway routing (once all pods are Running)
curl http://localhost/actuator/health
```

## Documentation

### High-Level Architecture

- [Architecture Overview](docs/architecture-overview.md)
- [Technology Stack](docs/technology-stack.md)
- [API Contracts](docs/api-contracts.md)
- [Event-Driven Patterns](docs/event-driven-patterns.md)
- [Observability Guide](docs/observability-guide.md)

### Internal Codebase Documentation

A comprehensive Javadoc site is generated during the build process to document domain invariants, application use cases, and infrastructure bindings.

To view the generated Java API documentation:

1. Run `mvn clean compile javadoc:aggregate` from the project root.
2. Open `target/site/apidocs/index.html` in your web browser.

## License

[MIT](LICENSE)
