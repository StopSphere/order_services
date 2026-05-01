# Order Service

REST API for order management. Handles order creation, tracking, and orchestrates calls to Inventory and User services with circuit breaker resilience.

## Tech Stack

- Java 21 · Spring Boot · Spring Data JPA
- OpenFeign · Resilience4j Circuit Breaker
- MySQL · MapStruct · Lombok · JWT Auth
- Eureka Client · Spring Cloud

## Quick Start

**Prerequisites:** Discovery Server (8761), MySQL (3307), Inventory Service, User Service

```powershell
# Start MySQL
docker compose up -d

# Run service
.\gradlew.bat bootRun
```

Service registers with Eureka as `order-service` on port 8082.

## Configuration

Edit `src/main/resources/application.yml`:

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
  jpa:
    hibernate:
      ddl-auto: update

eureka:
  client:
    service-url:
      defaultZone: http://host.docker.internal:8761/eureka/
    register-with-eureka: true
  instance:
    prefer-ip-address: true

resilience4j:
  circuit breaker:
    instances:
      productService:
        slidingWindowSize: 5
        failureRateThreshold: 50
        waitDurationInOpenState: 10s

jwt:
  secret: mysupersecretkeymysupersecretkey123
```

## API Endpoints

Base: `/v1/api/orders`

| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/` | Create order |
| GET | `/{orderId}` | Get order by ID |
| GET | `/` | List orders (paginated, sortable, filterable) |

### Query Parameters (List)

- `page`: 0-based (default: 0)
- `size`: page size (default: 5)
- `sortBy`: field name (default: orderId)
- `sortOrder`: `asc` or `desc` (default: asc)
- `status`: filter by OrderStatus (optional)
- `productId`: filter by product (optional)

### Examples

**Create Order**
```bash
curl -X POST http://localhost:8082/v1/api/orders \
  -H "Authorization: Bearer <jwt-token>" \
  -H "Content-Type: application/json" \
  -d '{
    "productId": "550e8400-e29b-41d4-a716-446655440000",
    "userId": "5f16d39a-7c88-4836-9380-0781732ffb03",
    "quantity": 2
  }'
```

**List Orders with Filters**
```bash
curl "http://localhost:8082/v1/api/orders?page=0&size=5&status=CREATED&sortBy=orderId&sortOrder=desc" \
  -H "Authorization: Bearer <jwt-token>"
```

**Get Single Order**
```bash
curl "http://localhost:8082/v1/api/orders/550e8400-e29b-41d4-a716-446655440000" \
  -H "Authorization: Bearer <jwt-token>"
```

### Response

**Order:**
```json
{
  "orderId": "660f9510-f30c-52e5-b827-557766551111",
  "userId": "5f16d39a-7c88-4836-9380-0781732ffb03",
  "productId": "550e8400-e29b-41d4-a716-446655440000",
  "quantity": 2,
  "status": "CREATED"
}
```

**Paginated Response:**
```json
{
  "content": [],
  "page": 0,
  "size": 5,
  "totalElements": 22,
  "totalPages": 5,
  "last": false
}
```

## DTOs & Entity

| DTO | Fields |
|-----|--------|
| CreateOrderRequestDTO | productId* · userId · quantity* |
| OrderResponseDTO | orderId · userId · productId · quantity · status |

*required fields

**OrderStatus enum:** CREATED, PROCESSING, COMPLETED, CANCELLED

**Order Entity fields:** orderId · userId · productId · quantity · status

## Service Integration

**InventoryClient** (Feign)
- GET `/v1/api/inventory/{productId}` — check stock
- PUT `/v1/api/inventory/remove` — deduct stock

**userClient** (Feign)
- Validates user existence before order creation

**Circuit Breaker** (Resilience4j)
- Detects failures in Inventory Service
- Opens after 50% failure rate on 5 requests
- Waits 10s before retry

## Project Structure

```
src/main/java/com/order_service/shopsphere/order_service/
├── Controller/         (REST endpoints)
├── Service/            (business logic)
├── Repository/         (data access)
├── Entity/             (JPA entities)
├── DTO/                (request/response DTOs)
├── Mapper/             (MapStruct mappers)
├── Client/             (Feign clients - Inventory, User)
├── Security/           (JWT auth, config)
├── Exception/          (global error handling)
└── OrderServiceApplication.java
```

## Tests

```powershell
.\gradlew.bat test
```

## Troubleshooting

| Issue | Solution |
|-------|----------|
| DB connection fails | Verify MySQL, check `application.yml` credentials |
| Order creation fails | Ensure User Service and Inventory Service are running |
| Circuit breaker trips | Inventory Service down, check error logs, waits 10s before retry |
| JWT auth errors | Token missing or invalid, include `Authorization: Bearer <token>` header |
| Eureka registration fails | Check discovery server at `http://localhost:8761` |

## Status

✅ CRUD operations (create, get, list)  
✅ Pagination & sorting with filtering  
✅ JWT authentication  
✅ OpenFeign service integration (User, Inventory)  
✅ Circuit breaker resilience (Inventory failures)  
✅ Eureka service discovery  
✅ Global exception handling  
✅ API Gateway ready  

## Architecture Notes

- **Service Orchestration:** Order Service calls User Service and Inventory Service
- **Circuit Breaker:** Handles Inventory Service failures gracefully
- **JWT Propagation:** Tokens passed through Feign interceptor for inter-service auth
- **Service Discovery:** Auto-registers with Eureka, discovers other services via registry
- **Database:** MySQL with JPA/Hibernate, auto schema updates
