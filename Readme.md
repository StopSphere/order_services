# Order Service

## Overview

This is the Order Service microservice for the ShopSphere e-commerce platform. It is responsible for handling order creation, validation, and managing the complete order lifecycle.

The service interacts with multiple microservices:

* **User Service** to validate user existence
* **Product Service** to verify stock and update inventory

It also works behind an **API Gateway** and supports **JWT-based authentication across services**.

---

## Features

* Create and manage orders with **user and product validation**
* Validate user existence via User Service before processing orders
* Communicate with Product Service using OpenFeign for stock management
* JWT token propagation across services for secure communication
* Track order lifecycle through states (CREATED, PROCESSING, COMPLETED, CANCELLED)
* Circuit Breaker integration for handling Product Service failures
* Structured logging with SLF4J for better observability
* Centralized exception handling for consistent error responses

---

## Tech Stack

* Java 21
* Spring Boot
* Spring Data JPA
* Spring Cloud OpenFeign
* Resilience4j (Circuit Breaker)
* MySQL
* Lombok
* SLF4J

---

## Project Structure

```
order-service/
├── controller/        REST API endpoints
├── service/           Business logic and orchestration
├── repository/        Database access layer
├── entity/            JPA entities
├── dto/               Request and response DTOs
├── mapper/            Entity-DTO mapping
├── client/            Feign clients (User, Product)
├── exception/         Global exception handling
└── config/            Security, Feign, and app configs
```

---

## How It Works

1. Client sends a request to create an order (via API Gateway)
2. JWT token is passed along with the request
3. Order Service validates the user by calling User Service
4. Token is propagated to User Service via Feign interceptor
5. If the user is valid, Product Service is called to reduce stock
6. If stock is available, the order is created and stored
7. If Product Service fails, Circuit Breaker handles fallback logic
8. If user validation fails, the request is rejected immediately

---

## API Endpoints

### Create an Order

```
POST /v1/api/orders
```

**Request:**

```json
{
  "userId": "5f16d39a-7c88-4836-9380-0781732ffb03",
  "productId": "550e8400-e29b-41d4-a716-446655440000",
  "quantity": 2
}
```

**Success Response (200):**

```json
{
  "id": "660f9510-f30c-52e5-b827-557766551111",
  "userId": "5f16d39a-7c88-4836-9380-0781732ffb03",
  "productId": "550e8400-e29b-41d4-a716-446655440000",
  "quantity": 2,
  "status": "CREATED"
}
```

---

## Error Handling

The service ensures meaningful and consistent error responses:

* **User Not Found / Unauthorized** - Invalid or unauthorized user
* **Product Not Found** - Product does not exist
* **Insufficient Stock** - Requested quantity unavailable
* **Service Unavailable** - Product Service failure handled via Circuit Breaker
* **Invalid Input** - Missing or incorrect request data

All exceptions are handled through a centralized global exception handler.

---

## Security & Communication

* JWT-based authentication is enforced across services
* Token propagation is implemented using a Feign RequestInterceptor
* API Gateway acts as a single entry point for all client requests
* Internal service communication is secured and consistent

---

## Architecture & Design

* **Microservices Architecture** with clear separation of concerns
* **Service Orchestration** handled by Order Service
* **Loose Coupling** using REST and Feign clients
* **Fault Tolerance** using Circuit Breaker (Resilience4j)
* **Secure Communication** using JWT token propagation
* **Centralized Routing** via API Gateway

---

## Getting Started

### Prerequisites

* Java 21+
* Gradle/Maven
* MySQL (or Docker setup)

---

### Running Locally

1. Clone the repository
2. Configure database in `application.yml`
3. Build the project:

   ```bash
   ./gradlew clean build
   ```
4. Run the service:

   ```bash
   ./gradlew bootRun
   ```

The service will start on `http://localhost:8082`

---

