# Order Service

REST API for managing orders in the e-commerce platform. Orchestrates order creation, status tracking, and coordinates with dependent services (User and Inventory) using both synchronous calls and asynchronous event-driven communication.

## Technology Stack

Java 21 · Spring Boot 3 · Spring Data JPA · OpenFeign · Resilience4J Circuit Breaker · MySQL 8 · Kafka · Eureka Client · MapStruct · JWT · Lombok

## Getting Started

### Prerequisites
- MySQL running on `localhost:3307`
- Kafka broker on `localhost:9092`
- Eureka Discovery Server on `localhost:8761`
- User Service running on port 8081
- Inventory Service running on port 8084

### Running the Service

```powershell
# Start supporting services
docker compose up -d

# Build and run
.\gradlew.bat clean build
.\gradlew.bat bootRun
```

The service starts on port 8082 and registers with Eureka as `order-service`.

## Configuration

Service configuration is defined in `src/main/resources/application.yml`:

```yaml
server:
  port: 8082

spring:
  application:
    name: order-service
    
  datasource:
    url: jdbc:mysql://localhost:3307/order_db
    username: root
    password: 1234
    driver-class-name: com.mysql.cj.jdbc.Driver
    
  jpa:
    hibernate:
      ddl-auto: update
    show-sql: true
    
  kafka:
    bootstrap-servers: localhost:9092
    producer:
      key-serializer: org.apache.kafka.common.serialization.StringSerializer
      value-serializer: org.springframework.kafka.support.serializer.JsonSerializer
    consumer:
      group-id: order-service-group
      auto-offset-reset: earliest
      key-deserializer: org.apache.kafka.common.serialization.StringDeserializer
      value-deserializer: org.springframework.kafka.support.serializer.JsonDeserializer
      properties:
        spring.json.trusted.packages: "*"
        spring.json.value.default.type: com.order_service.shopsphere.order_service.DTO.Event.InventoryReservedEvent
    properties:
      spring.json.add.type.headers: false

eureka:
  client:
    service-url:
      defaultZone: http://host.docker.internal:8761/eureka/
    register-with-eureka: true
    fetch-registry: true
  instance:
    prefer-ip-address: true

resilience4j:
  circuit breaker:
    instances:
      inventoryService:
        slidingWindowSize: 5
        failureRateThreshold: 50
        waitDurationInOpenState: 10s

jwt:
  secret: mysupersecretkeymysupersecretkey123
```

## REST API

### Base URL
`/v1/api/orders`

### Endpoints

| Method | Path | Description | Auth Required |
|--------|------|-------------|---------------|
| POST | `/` | Create new order | Yes |
| GET | `/{orderId}` | Retrieve order details | Yes |
| GET | `/` | List orders with filters | Yes |

### Creating an Order

**Request**
```http
POST /v1/api/orders HTTP/1.1
Authorization: Bearer <jwt-token>
Content-Type: application/json

{
  "productId": "550e8400-e29b-41d4-a716-446655440000",
  "quantity": 2
}
```

**Response** (HTTP 200)
```json
{
  "orderId": "660f9510-f30c-52e5-b827-557766551111",
  "userId": "5f16d39a-7c88-4836-9380-0781732ffb03",
  "productId": "550e8400-e29b-41d4-a716-446655440000",
  "quantity": 2,
  "status": "CREATED"
}
```

### Listing Orders

Query parameters control the result set:

- `page` (default: 0) - zero-based page number
- `size` (default: 5) - records per page
- `sortBy` (default: orderId) - field to sort by
- `sortOrder` (default: asc) - sort direction (asc or desc)
- `status` (optional) - filter by CREATED, PROCESSING, CONFIRMED, CANCELLED
- `productId` (optional) - filter by product UUID

**Example**
```bash
curl -H "Authorization: Bearer <token>" \
  "http://localhost:8082/v1/api/orders?page=0&size=10&status=CONFIRMED&sortBy=orderId&sortOrder=desc"
```

**Response** (HTTP 200)
```json
{
  "content": [
    {
      "orderId": "660f9510-f30c-52e5-b827-557766551111",
      "userId": "5f16d39a-7c88-4836-9380-0781732ffb03",
      "productId": "550e8400-e29b-41d4-a716-446655440000",
      "quantity": 2,
      "status": "CONFIRMED"
    }
  ],
  "page": 0,
  "size": 10,
  "totalElements": 42,
  "totalPages": 5,
  "last": false
}
```

### Retrieving a Single Order

```bash
curl -H "Authorization: Bearer <token>" \
  http://localhost:8082/v1/api/orders/660f9510-f30c-52e5-b827-557766551111
```

## Service Integration

The Order Service integrates with other microservices via Feign clients and circuit breaker patterns.

### User Service

Validates that the requesting user exists. Called during order creation as a synchronous check.

```
GET /v1/api/users/{userId} → User Service
```

### Inventory Service

Synchronous calls for stock validation during order placement. The actual stock deduction is delegated to async Kafka events to avoid long-running transactions.

```
GET /v1/api/inventory/{productId} → Check stock
```

### Circuit Breaker Pattern

The Inventory Service integration uses Resilience4J circuit breaker to handle transient failures gracefully. After 5 consecutive failures or 50% failure rate, the circuit opens and fails fast. It waits 10 seconds before attempting to retry.

## Event-Driven Architecture

This service implements the saga pattern for distributed order processing. Orders are created in a CREATED state, then the inventory service reserves stock asynchronously via Kafka events.

### Published Events

**OrderCreatedEvent** to `order-created` topic

Published immediately after an order is successfully created and saved to the database. The inventory service consumes this event to reserve stock.

- Fields: `orderId`, `productId`, `quantity`
- Key: `orderId` (ensures ordering within a partition)

### Consumed Events

**InventoryReservedEvent** from `inventory-reserved` topic

Received when the inventory service successfully reserved the requested stock. The order status is updated to CONFIRMED.

- Listener Group: `order-service-group`
- Field: `orderId`
- Action: Order status → CONFIRMED

**InventoryFailedEvent** from `inventory-failed` topic

Received when inventory cannot fulfill the request (product not found or insufficient stock). The order is cancelled.

- Listener Group: `order-service-group`
- Fields: `orderId`, `reason`
- Action: Order status → CANCELLED

### Order Fulfillment Flow

```
POST /v1/api/orders
        ↓
Validate user exists (sync)
        ↓
Create Order (status=CREATED)
        ↓
Publish OrderCreatedEvent
        ↓
     [Kafka]
        ↓
Inventory Service consumes event
    ├─ Check product exists
    ├─ Verify quantity available
    ├─ Deduct from inventory
    │
    ├─ Success → InventoryReservedEvent
    │              ↓
    │         Order status → CONFIRMED
    │
    └─ Failure → InventoryFailedEvent
                  ↓
             Order status → CANCELLED
```

## Data Models

### Order Entity

```java
{
  orderId: UUID,
  userId: UUID,
  productId: UUID,
  quantity: int,
  status: OrderStatus enum
}
```

**OrderStatus values:** CREATED, PROCESSING, CONFIRMED, CANCELLED

### Request DTOs

**CreateOrderRequestDTO**
```json
{
  "productId": "UUID (required)",
  "quantity": "int (required)"
}
```

### Response DTOs

**OrderResponseDTO**
```json
{
  "orderId": "UUID",
  "userId": "UUID",
  "productId": "UUID",
  "quantity": "int",
  "status": "enum"
}
```

**PagedResponse<OrderResponseDTO>**
```json
{
  "content": [OrderResponseDTO],
  "page": "int",
  "size": "int",
  "totalElements": "long",
  "totalPages": "int",
  "last": "boolean"
}
```

## Project Structure

```
src/main/java/com/order_service/shopsphere/order_service/
├── Controller/
│   └── OrderController.java
├── Service/
│   ├── OrderService.java
│   └── Impl/
│       └── OrderServiceImpl.java
├── Repository/
│   └── OrderRepository.java
├── Entity/
│   └── Order.java
├── Kafka/
│   ├── OrderEventProducer.java
│   └── InventoryEventConsumer.java
├── Client/
│   ├── InventoryClient.java
│   └── UserClient.java
├── DTO/
│   ├── Request/
│   │   └── CreateOrderRequestDTO.java
│   ├── Response/
│   │   ├── OrderResponseDTO.java
│   │   └── PagedResponse.java
│   └── Event/
│       ├── OrderCreatedEvent.java
│       ├── InventoryReservedEvent.java
│       └── InventoryFailedEvent.java
├── Mapper/
│   └── OrderMapper.java
├── Security/
│   └── SecurityConfig.java
├── Exception/
│   └── GlobalExceptionHandler.java
└── OrderServiceApplication.java
```

## Security

JWT authentication is required for all API endpoints. Include the bearer token in the Authorization header:

```
Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
```

The token is validated against the JWT secret defined in configuration. User ID is extracted from the token claims and used for authorization checks.

## Error Handling

The service defines specific error responses for common failure scenarios:

| Scenario | Status | Message |
|----------|--------|---------|
| Missing/invalid JWT | 401 | Unauthorized |
| User not found | 404 | User does not exist |
| Order not found | 404 | Order not found |
| Invalid order data | 400 | Validation failed |
| Inventory service timeout | 503 | Service temporarily unavailable |

## Testing

Run the full test suite:

```powershell
.\gradlew.bat test
```

## Troubleshooting

**Order creation fails with "User not found"**
- Verify the requesting user ID is valid
- Ensure the User Service is running and accessible

**Orders stuck in CREATED status**
- Check that Inventory Service is running
- Verify Kafka is running and topics exist
- Check logs for deserialization errors in event consumers
- Inspect inventory database to confirm stock availability

**Circuit breaker is open**
- Indicates repeated failures calling Inventory Service
- Check Inventory Service logs and health
- Wait 10 seconds for the circuit to attempt retry

**JWT authentication errors**
- Verify token is included in Authorization header
- Ensure token format: `Bearer <token>`
- Check token expiration and validity

## Performance Considerations

- Order creation is fast since inventory deduction is asynchronous
- Status updates occur when Kafka events are consumed (near real-time)
- All Kafka consumers are transactional for consistency
- Message ordering is preserved per order using orderId as the Kafka key
- Database operations benefit from connection pooling

## Implementation Details

- Event consumers use Spring's @Transactional to ensure atomicity
- Feign clients include automatic request logging for debugging
- Circuit breaker prevents cascading failures to dependent services
- All events are serialized as JSON with proper type information
- Service discovery via Eureka enables dynamic service location

## Status

- Order creation with user and inventory validation
- Complete order lifecycle management
- Pagination, sorting, and filtering on order lists
- JWT authentication on all endpoints
- Synchronous User Service integration
- Asynchronous Inventory Service integration via Kafka
- Resilience4J circuit breaker for fault tolerance
- Saga pattern for distributed transactions
- Eureka service registration
- Comprehensive error handling
- Structured logging throughout
