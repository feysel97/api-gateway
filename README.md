# Spring Cloud API Gateway

The centralized entry point and security enforcer for the microservices ecosystem.

This reactive gateway handles intelligent request routing, global CORS configuration, and acts as the primary defense layer. It intercepts incoming requests, validates JWT access tokens statelessly, and mutates downstream requests to inject secure identity headers, keeping internal microservices lightweight.

## 🚀 Key Features

* **Reactive Routing:** Built on Spring WebFlux for non-blocking, high-throughput request proxying.
* **Global JWT Validation:** A custom `GlobalFilter` intercepts all protected routes, verifies the JWT signature against the shared secret, and checks token expiration.
* **Secure Header Injection:** Extracts claims from the validated JWT (email, ID, roles) and injects them as trusted HTTP headers (`X-User-Email`, `X-User-Id`, `X-User-Roles`) for downstream services to consume.
* **Public Route Bypassing:** Explicitly configured bypass lists to allow traffic to authentication and registration endpoints without token evaluation.
* **Centralized CORS Management:** Handles Cross-Origin Resource Sharing globally so downstream services do not need independent CORS configurations.

## 🛠️ Tech Stack

* **Runtime:** Java 21 (LTS)
* **Framework:** Spring Boot 3.4.2, Spring Cloud Gateway
* **Security:** JJWT (0.12.5) for stateless token parsing
* **Build Tool:** Gradle 8.5+

## 🛤️ Routing Configuration

| Path Pattern | Downstream Service | JWT Required |
|---|---|---|
| `/api/auth/**` | `auth-service` | No (Public) |
| `/api/v1/users/**` | `user-service` | Yes |
| `/api/v1/notifications/**` | `notification-service` | Yes |