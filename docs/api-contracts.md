# API Contracts

## Auth Service

**Purpose**: OAuth2 authorization server; issues JWT tokens, manages user authentication, handles refresh tokens.

| Method | Endpoint         | Description              | Request Body                                                                   | Response                                                                     |
| ------ | ---------------- | ------------------------ | ------------------------------------------------------------------------------ | ---------------------------------------------------------------------------- |
| POST   | `/auth/register` | Register new user        | `{ "username": "john", "email": "john@example.com", "password": "secret123" }` | `{ "id": "uuid", "username": "john" }`                                       |
| POST   | `/auth/login`    | Login and get tokens     | `{ "username": "john", "password": "secret123" }`                              | `{ "access_token": "jwt...", "refresh_token": "rt...", "expires_in": 3600 }` |
| POST   | `/auth/refresh`  | Refresh access token     | `{ "refresh_token": "rt..." }`                                                 | `{ "access_token": "jwt...", "expires_in": 3600 }`                           |
| POST   | `/auth/logout`   | Invalidate refresh token | `{ "refresh_token": "rt..." }`                                                 | `{ "message": "Logged out successfully" }`                                   |
| GET    | `/auth/me`       | Get current user info    | - (requires JWT)                                                               | `{ "id": "uuid", "username": "john", "roles": ["ROLE_USER"] }`               |

## Order Service

**Purpose**: Core business logic for order management; orchestrates saga pattern, publishes order events, implements outbox pattern.

| Method | Endpoint              | Description        | Request Body                                          | Response                                                                     |
| ------ | --------------------- | ------------------ | ----------------------------------------------------- | ---------------------------------------------------------------------------- |
| POST   | `/orders`             | Create new order   | `{ "items": [{"productId": "uuid", "quantity": 2}] }` | `{ "orderId": "uuid", "status": "PENDING", "total": 99.98 }`                 |
| GET    | `/orders`             | List user's orders | Query: `?page=0&size=20`                              | `{ "content": [...], "totalPages": 5 }`                                      |
| GET    | `/orders/{id}`        | Get order details  | -                                                     | `{ "id": "uuid", "status": "CONFIRMED", "items": [...] }`                    |
| PUT    | `/orders/{id}/cancel` | Cancel order       | -                                                     | `{ "id": "uuid", "status": "CANCELLED" }`                                    |
| GET    | `/orders/{id}/status` | Get order status   | -                                                     | `{ "id": "uuid", "status": "SHIPPED", "updatedAt": "2026-02-26T10:00:00Z" }` |

## Inventory Service

**Purpose**: Product catalog management, stock tracking, inventory reservations; consumes order events to update stock levels.

| Method | Endpoint                      | Description              | Request Body                                                | Response                                                         |
| ------ | ----------------------------- | ------------------------ | ----------------------------------------------------------- | ---------------------------------------------------------------- |
| POST   | `/products`                   | Create product (ADMIN)   | `{ "name": "Mouse", "price": 29.99, "stockQuantity": 100 }` | `{ "productId": "uuid", "name": "Mouse" }`                       |
| GET    | `/products`                   | List products            | Query: `?category=Electronics&page=0&size=20`               | `{ "content": [...], "totalPages": 5 }`                          |
| GET    | `/products/{id}`              | Get product details      | -                                                           | `{ "productId": "uuid", "name": "Mouse", "stockQuantity": 100 }` |
| PUT    | `/products/{id}`              | Update product (ADMIN)   | `{ "price": 24.99 }`                                        | `{ "productId": "uuid", "price": 24.99 }`                        |
| GET    | `/products/{id}/availability` | Check stock              | -                                                           | `{ "productId": "uuid", "availableQuantity": 95 }`               |
| POST   | `/products/{id}/reserve`      | Reserve stock (internal) | `{ "orderId": "uuid", "quantity": 2 }`                      | `{ "reservationId": "uuid", "reserved": true }`                  |

## Analytics Service (GraphQL)

**Purpose**: Real-time business metrics aggregation; GraphQL API for flexible querying; time-series data storage.

### GraphQL Schema

```graphql
type Query {
  orderMetrics(
    startTime: String!
    endTime: String!
    status: String
  ): [OrderMetric!]!

  inventoryMetrics(
    startTime: String!
    endTime: String!
    category: String
  ): [InventoryMetric!]!

  realtimeDashboard: DashboardData!
}

type OrderMetric {
  timestamp: String!
  status: String!
  count: Int!
  totalAmount: Float!
}

type InventoryMetric {
  timestamp: String!
  category: String!
  stockQuantity: Int!
  reservedQuantity: Int!
}

type DashboardData {
  totalOrders: Int!
  totalRevenue: Float!
  activeUsers: Int!
  lowStockProducts: [LowStockProduct!]!
}

type LowStockProduct {
  productId: String!
  name: String!
  stockQuantity: Int!
}
```
