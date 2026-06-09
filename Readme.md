# Order Service

A Spring Boot microservice that manages the complete order lifecycle in the ShopSphere platform using event-driven architecture and the Saga pattern.

---

## Tech Stack

| Component | Technology |
|-----------|-----------|
| Language | Java 21 |
| Framework | Spring Boot 4.0.6 |
| Messaging | Apache Kafka |
| Security | Spring Security + JWT |
| HTTP Client | OpenFeign |
| Resilience | Resilience4j Circuit Breaker |
| Database | MySQL 8 |
| Mapping | MapStruct |
| Testing | JUnit 5, Mockito |

---

## Kafka Event Flow

```
Client
  │
  │ POST /v1/api/orders
  ▼
Order Service
  │  Save order (status = CREATED)
  │  Publish ──► order-created ──► Inventory Service
  │
  ├── inventory-reserved ◄── Inventory Service
  │       │  (log only — waits for payment)
  │
  ├── inventory-failed ◄── Inventory Service
  │       │  Order → CANCELLED
  │
  ├── payment-success ◄── Payment Service
  │       │  Order → CONFIRMED
  │
  └── payment-failed ◄── Payment Service
          │  Order → CANCELLED
```

**Full Saga (Happy Path):**
```
Order CREATED → inventory-reserved → payment-success → CONFIRMED
```

**Compensation Paths:**
```
Order CREATED → inventory-failed              → CANCELLED
Order CREATED → inventory-reserved → payment-failed → CANCELLED
```

---

## Key Design Patterns

| Pattern | Implementation |
|---------|---------------|
| **Saga (Choreography)** | Order status driven entirely by Kafka events from Inventory & Payment Services |
| **Circuit Breaker** | Resilience4j on order creation — 50% failure rate threshold, fallback returns 503 |
| **Idempotent Consumer** | `@Transactional` on all Kafka listeners ensures safe reprocessing |

---

## API Endpoints

| Method | Endpoint | Description | Auth |
|--------|----------|-------------|------|
| `POST` | `/v1/api/orders` | Create a new order | JWT |
| `GET` | `/v1/api/orders/{orderId}` | Get order by ID | JWT |
| `GET` | `/v1/api/orders` | List orders (paginated, filtered) | JWT |
| `GET` | `/v1/api/orders/user/{userId}` | Get orders by user | JWT |

**List Orders Query Params:**
- `page`, `size`, `sortBy`, `sortOrder`
- `status` — filter by `CREATED`, `CONFIRMED`, `CANCELLED`
- `productId` — filter by product UUID

---

## Order Lifecycle

```
CREATED ──► (inventory-reserved) ──► (payment-success) ──► CONFIRMED
   │
   ├──► (inventory-failed) ──► CANCELLED
   │
   └──► (payment-failed)  ──► CANCELLED
```

---

## Testing

Unit tests written with **JUnit 5** and **Mockito**.

**Coverage includes:**
- Order creation and validation
- Kafka event publishing
- Inventory & Payment event consumption
- Circuit breaker fallback
- Exception handling

```bash
.\gradlew test
# Report: build/reports/tests/test/index.html
```

---

## How to Run

**Prerequisites:** Java 21, MySQL on `localhost:3307`, Kafka on `localhost:9092`, Eureka on `localhost:8761`

```bash
docker compose up -d
.\gradlew clean build
.\gradlew bootRun
```

Service runs on `http://localhost:8082`, registers with Eureka as `order-service`.