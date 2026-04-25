# Order Service

## Overview

Order Service is a backend microservice responsible for handling order creation and managing order lifecycle in the ShopSphere system.

It communicates with the Product Service to validate and update stock before confirming an order.

---

## Features

* Create orders with product validation
* Communicate with Product Service using OpenFeign
* Maintain order lifecycle states:

    * CREATED
    * PROCESSING
    * COMPLETED
    * CANCELLED
* Handle external service failures gracefully
* Centralized exception handling
* Structured logging for debugging and monitoring

---

## Tech Stack

* Java 21
* Spring Boot
* Spring Data JPA
* OpenFeign
* MySQL
* Lombok
* SLF4J

---

## Project Structure

```
order-service
├── controller
├── service
├── repository
├── entity
├── dto
├── mapper
├── client
├── exception
└── config
```

---

## Order Flow

1. Client sends a request to create an order
2. Order Service calls Product Service to reduce stock
3. Based on response:

    * Success → order moves to CREATED
    * Failure → order handling logic is applied
4. Order is stored in database

---

## API

### Create Order

```
POST /v1/api/orders
```

#### Request

```json
{
  "productId": "uuid",
  "quantity": 2
}
```

#### Response

```json
{
  "id": "uuid",
  "productId": "uuid",
  "quantity": 2,
  "status": "CREATED"
}
```

---

## Error Handling

The service handles failures from external services and maps them to meaningful responses such as:

* Product not found
* Insufficient stock
* Service unavailable

---

## Notes

* Service-to-service communication is implemented using Feign client
* The system is designed to remain loosely coupled
* Logging is added for better traceability

---

## TODOS

* Circuit Breaker (Resilience4j)
* Service discovery (Eureka)
* API Gateway integration
* Docker-based deployment
