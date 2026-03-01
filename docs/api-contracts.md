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

**Purpose**: Core business logic for order management; publishes domain events via Transactional Outbox Pattern, implements saga-ready architecture.

**Base Path**: `/api/v1/orders`

### Implemented Endpoints

| Method | Endpoint              | Description        | Request Body                                                              | Response                                                                                                                                       |
| ------ | --------------------- | ------------------ | ------------------------------------------------------------------------- | ---------------------------------------------------------------------------------------------------------------------------------------------- |
| POST   | `/api/v1/orders`      | Create new order   | `{ "items": [{"productId": "uuid", "quantity": 2, "unitPrice": 10.00}] }` | `201`: `{ "id": "uuid", "userId": "uuid", "status": "PENDING", "totalAmount": 20.00, "items": [...], "createdAt": "...", "updatedAt": "..." }` |
| GET    | `/api/v1/orders`      | List user's orders | - (userId extracted from JWT subject)                                     | `200`: `[{ "id": "uuid", "userId": "uuid", "status": "PENDING", "totalAmount": 20.00, "items": [...] }]`                                       |
| GET    | `/api/v1/orders/{id}` | Get order details  | -                                                                         | `200`: `{ "id": "uuid", ... }` or `404`                                                                                                        |

### Planned Endpoints

| Method | Endpoint                     | Description      | Status  |
| ------ | ---------------------------- | ---------------- | ------- |
| PUT    | `/api/v1/orders/{id}/cancel` | Cancel order     | Planned |
| GET    | `/api/v1/orders/{id}/status` | Get order status | Planned |

## Inventory Service

**Purpose**: Product catalog management, stock tracking, inventory reservations; consumes order events to update stock levels.

**Base Path**: `/api/v1/products`

| Method | Endpoint                        | Description              | Request Body                                                | Response                                                         |
| ------ | ------------------------------- | ------------------------ | ----------------------------------------------------------- | ---------------------------------------------------------------- |
| POST   | `/api/v1/products`              | Create product (ADMIN)   | `{ "name": "Mouse", "price": 29.99, "stockQuantity": 100 }` | `{ "productId": "uuid", "name": "Mouse" }`                       |
| GET    | `/api/v1/products`              | List products            | Query: `?category=Electronics&page=0&size=20`               | `{ "content": [...], "totalPages": 5 }`                          |
| GET    | `/api/v1/products/{id}`         | Get product details      | -                                                           | `{ "productId": "uuid", "name": "Mouse", "stockQuantity": 100 }` |
| PUT    | `/api/v1/products/{id}`         | Update product (ADMIN)   | `{ "price": 24.99 }`                                        | `{ "productId": "uuid", "price": 24.99 }`                        |
| GET    | `/api/v1/products/{id}/avail`   | Check stock              | -                                                           | `{ "productId": "uuid", "availableQuantity": 95 }`               |
| POST   | `/api/v1/products/{id}/reserve` | Reserve stock (internal) | `{ "orderId": "uuid", "quantity": 2 }`                      | `{ "reservationId": "uuid", "reserved": true }`                  |

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
