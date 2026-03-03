# Architecture Overview

## High-Level Architecture

```
┌─────────────────────────────────────────────────────────────────────────┐
│                          External Clients                                │
│                     (Postman, cURL, Integration Tests)                   │
└────────────────────────────────┬────────────────────────────────────────┘
                                 │
                                 │ HTTP/HTTPS
                                 ▼
                    ┌────────────────────────┐
                    │  Nginx Ingress (K8s)   │
                    └──────────┬─────────────┘
                               │
                               ▼
                    ┌────────────────────────┐
                    │   API Gateway          │
                    │   (Spring Cloud)       │
                    │   - Rate Limiting      │
                    │   - JWT Validation     │
                    │   - Circuit Breaking   │
                    └──────────┬─────────────┘
                               │
                ┌──────────────┼──────────────┐
                │              │              │
                ▼              ▼              ▼
       ┌────────────┐  ┌────────────┐  ┌────────────┐
       │ Auth       │  │ Order      │  │ Inventory  │
       │ Service    │  │ Service    │  │ Service    │
       │            │  │            │  │            │
       │ PostgreSQL │  │ PostgreSQL │  │ MongoDB    │
       └─────┬──────┘  └─────┬──────┘  └─────┬──────┘
             │               │               │
             │               │ Publish       │ Subscribe
             │               │ Events        │ Events
             │               ▼               │
             │      ┌─────────────────┐     │
             │      │   RabbitMQ      │◄────┘
             │      │   Message Broker│
             │      └────────┬────────┘
             │               │
             └───────────────┼────────────────────┐
                             │                    │
                    ┌────────▼────────┐  ┌────────▼────────┐
                    │ Notification    │  │ Analytics       │
                    │ Service         │  │ Service         │
                    │ (Email/SMS)     │  │ (GraphQL API)   │
                    └─────────────────┘  └────────┬────────┘
                                                   │
                                         ┌─────────▼─────────┐
                                         │ InfluxDB          │
                                         │ (Time-Series)     │
                                         └───────────────────┘

┌─────────────────────────────────────────────────────────────────────────┐
│                    Observability Stack (Cross-Cutting)                   │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐  ┌─────────────┐│
│  │ Jaeger       │  │ Prometheus   │  │ Grafana      │  │ ELK Stack   ││
│  │ (Tracing)    │  │ (Metrics)    │  │ (Dashboards) │  │ (Logs)      ││
│  └──────────────┘  └──────────────┘  └──────────────┘  └─────────────┘│
└─────────────────────────────────────────────────────────────────────────┘
```

## Service Boundaries (Domain-Driven Design)

| Service                  | Bounded Context       | Aggregate Root | Responsibilities                                                |
| ------------------------ | --------------------- | -------------- | --------------------------------------------------------------- |
| **Auth Service**         | Identity & Access     | User           | User registration, authentication, token issuance               |
| **Order Service**        | Order Management      | Order          | Order creation, status updates, saga orchestration              |
| **Inventory Service**    | Stock Management      | Product        | Product catalog, stock tracking, reservations                   |
| **Notification Service** | Communication         | Notification   | Email/SMS delivery, notification history                        |
| **Analytics Service**    | Business Intelligence | Metric         | Real-time metrics, GraphQL queries, dashboards                  |
| **API Gateway**          | Edge Service          | N/A            | Routing, auth, rate limiting, circuit breaking                  |
| **Shared Library**       | Cross-Cutting         | Events         | Centralized definitions for shared Types to prevent duplication |

## Communication Patterns

### Synchronous (REST API)

```
Client → Gateway → Order Service (HTTP/REST)
Client → Gateway → Inventory Service (HTTP/REST)
```

### Asynchronous (Message Broker)

```
Order Service → RabbitMQ → Inventory Service (Event: OrderCreated)
Order Service → RabbitMQ → Notification Service (Event: OrderConfirmed)
Inventory Service → RabbitMQ → Analytics Service (Event: StockUpdated)
```

### Kubernetes Service Discovery

```
API Gateway → Kubernetes DNS → Microservice (e.g. auth-service:8081)
```

The system relies on native Kubernetes `ClusterIP` services rather than Spring Cloud Eureka. The API Gateway routes are statically defined via `SPRING_APPLICATION_JSON` pointing to internal K8s DNS addresses.
