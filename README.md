# Financial Profile API ☕

![CI](https://github.com/emiisomoza/finantial-profile-api/actions/workflows/ci.yml/badge.svg)
![Java](https://img.shields.io/badge/java-21-orange)
![Spring Boot](https://img.shields.io/badge/spring--boot-4.0-green)
![License](https://img.shields.io/badge/license-MIT-green)

REST API built in **Java (Spring Boot)** that manages users, assets, incomes, expenses and financial summaries for a personal wealth management platform. Uses **Hexagonal Architecture** to keep domain logic isolated from infrastructure concerns.

This service is part of a larger financial portfolio system:
- ☕ **Financial Profile API** (this repo) — Java/Spring Boot: manages users, income, expenses and assets
- 💱 **[Price API](https://github.com/emiisomoza/price-api)** — Ruby/Sinatra: resolves real-time asset prices
- 🐍 **[financial-summary-worker](https://github.com/emiisomoza/financial-summary-worker)** — Python: consumes a queue and sends summary emails

---

## Architecture — Hexagonal

The codebase is split into three layers with strict dependency rules:

```
domain/          → Entities, ports (interfaces), domain exceptions — no framework deps
application/     → Use cases (services) — depends only on domain
infrastructure/  → Web controllers, persistence, messaging, scheduling — depends on application
```

---

## Endpoints

### Users

```
POST   /api/v1/users
GET    /api/v1/users/{id}
PUT    /api/v1/users/{id}/promote
POST   /api/v1/auth/login
```

### Assets

```
POST   /api/v1/assets
GET    /api/v1/assets?userId={id}
GET    /api/v1/assets/{id}
PUT    /api/v1/assets/{id}
DELETE /api/v1/assets/{id}
```

### Incomes

```
POST   /api/v1/incomes
GET    /api/v1/incomes?userId={id}
PUT    /api/v1/incomes/{id}
DELETE /api/v1/incomes/{id}
```

### Expenses

```
POST   /api/v1/expenses
GET    /api/v1/expenses?userId={id}
PUT    /api/v1/expenses/{id}
DELETE /api/v1/expenses/{id}
```

### Financial Summary

```
GET    /api/v1/summary/{userId}?currency={currency}
```

Response:
```json
{
  "userId": "3fa85f64-5717-4562-b3fc-2c963f66afa6",
  "currency": "AUD",
  "totalAssetsValue": 150000.00,
  "monthlyIncome": 5000.00,
  "monthlyExpenses": 3500.00,
  "monthlySavings": 1500.00,
  "savingsRate": 0.3,
  "unpricedAssetsCount": 0
}
```

### Summary Subscriptions

```
POST   /api/v1/summary-subscriptions
GET    /api/v1/summary-subscriptions/user/{userId}
PUT    /api/v1/summary-subscriptions/{id}
DELETE /api/v1/summary-subscriptions/{id}
```

Supported frequencies: `WEEKLY` (sends next Monday), `MONTHLY` (sends first day of next month).

---

## Tech Stack

| | |
|---|---|
| Language | Java 21 |
| Framework | Spring Boot 4.0 |
| Persistence | Spring Data JDBC + PostgreSQL |
| Messaging | Spring AMQP + RabbitMQ |
| Validation | Jakarta Bean Validation |
| Security | Spring Security Crypto (BCrypt) |
| Testing | JUnit 5 + Mockito + TestContainers |
| CI | GitHub Actions + JaCoCo (85% coverage gate) |

---

## Integrations

### Ruby Price API
Used to fetch real-time prices for stocks, crypto and FX rates.
The Java API communicates through a `MarketPricePort` with an HTTP adapter (`PriceApiClient`).

```
GET /v1/price?assetType=stock&asset=AAPL&currency=AUD
```

### Python Notifier
Receives financial summary messages from RabbitMQ and sends them as email reports to users.
The Java API publishes to the `summary.notifications` queue via a `SummaryPublisherPort`.

---

## Run locally

### Prerequisites
- Java 21+
- Maven
- Docker (for PostgreSQL and RabbitMQ)

### Setup
```bash
git clone https://github.com/emiisomoza/finantial-profile-api.git
cd finantial-profile-api
```

### Start dependencies
```bash
docker compose up -d
```

### Start the server
```bash
mvn spring-boot:run
```

API available at `http://localhost:8080`

### Health check
```bash
GET http://localhost:8080/actuator/health
```

---

## Run tests

```bash
# Unit and integration tests
mvn test

# Tests + coverage report (enforces 85% gate)
mvn verify
```

Coverage report is generated at `target/site/jacoco/index.html`.

---

## Project structure

```
finantial-profile-api/
├── .github/
│   └── workflows/
│       └── ci.yml                   # GitHub Actions CI + coverage gate
├── src/main/java/.../
│   ├── domain/
│   │   ├── model/                   # Entities and value objects
│   │   ├── ports/                   # Outbound port interfaces
│   │   ├── exceptions/              # Domain exceptions
│   │   └── validation/              # Domain validators
│   ├── application/
│   │   └── usecases/                # UserService, AssetService, SummaryService, etc.
│   └── infrastructure/
│       ├── config/                  # Spring beans (RabbitMQ, passwords)
│       ├── messaging/               # SummaryPublisher (RabbitMQ)
│       ├── persistence/             # Spring Data JDBC repositories
│       ├── pricing/                 # PriceApiClient (HTTP → Ruby API)
│       ├── scheduling/              # SummaryScheduler (daily cron)
│       └── web/                     # Controllers, DTOs, filters
├── src/main/resources/
│   ├── application.properties
│   ├── schema.sql                   # DB schema (auto-applied on startup)
│   └── logback-spring.xml           # Structured JSON request logging
├── src/test/
├── compose.yaml                     # PostgreSQL + RabbitMQ
└── pom.xml
```

---

## License

MIT
