# Financial Profile API (Java / Spring Boot)

The Financial Profile API is the main backend service of a personal wealth and savings management platform.  
Users can register, add assets, incomes, and expenses, and generate a financial summary including net worth, monthly income/expenses, saving capacity, and real-time market valuations.

This service forms the core of a 3-service ecosystem:

1. Java API — Financial Profile (this service)  
2. Python API — Market Valuation (prices for crypto/stocks)  
3. Ruby API — Email Reporting Service  

---

## Table of Contents

- [Overview](#overview)
- [Features](#features)
- [Data Model](#data-model)
- [API Endpoints](#api-endpoints)
- [Integrations](#integrations)
- [Running the Project](#running-the-project)
- [Docker Support](#docker-support)
- [Testing](#testing)
- [Roadmap](#roadmap)
- [License](#license)

---

## Overview

The Financial Profile API manages:

- User accounts  
- Assets (properties, cash, stocks, crypto, etc.)  
- Incomes and expenses  
- Financial summary calculations  

It also communicates with:

- The Python Market Valuation API to fetch live asset values  
- The Ruby Email Service to send summary reports  

This service uses Hexagonal Architecture to maintain clean separation between domain logic, application use cases, and infrastructure.

---

## Features

- User registration and profile management  
- CRUD operations for assets, incomes, and expenses  
- Market-based valuation for stocks and cryptocurrencies  
- Calculation of:
  - Net worth  
  - Monthly income  
  - Monthly expenses  
  - Saving capacity  
  - Saving rate percentage  
- Modular architecture allowing easy extension  
- Integration with external services (Python and Ruby)  
- Docker

---

## Data Model

### User
- id  
- email  
- passwordHash  
- fullName  
- createdAt  
- updatedAt  

### Asset
- id  
- userId  
- type (PROPERTY, VEHICLE, CASH, STOCK, CRYPTO, FUND, OTHER)  
- name  
- symbol (optional, for market assets)  
- quantity  
- valuationMode (MARKET or MANUAL)  
- manualUnitValue  
- currency  
- createdAt  
- updatedAt  

### Income
- id  
- userId  
- description  
- amount  
- frequency (MONTHLY, WEEKLY, YEARLY, ONCE)  
- category  
- currency  
- startDate  
- endDate  

### Expense
- Same structure as Income  

---

## API Endpoints

### Users

```
POST   /api/v1/users
GET    /api/v1/users/{id}
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
GET /api/v1/summary/{userId}
```

Example Response:

```json
{
  "userId": "uuid",
  "asOf": "2025-01-01T00:00:00Z",
  "netWorth": 123456.78,
  "monthlyIncome": 3000.0,
  "monthlyExpenses": 2200.0,
  "savingCapacity": 800.0,
  "savingRatePercent": 26.7
}
```

---

## Integrations

### Python Market Valuation API
Used to fetch real-time stock and crypto prices.  
The Java API communicates through a MarketPricePort with an HTTP adapter.

### Ruby Email Report API
Used to send financial summary emails.  

---

## Running the Project

### Requirements
- Java 21 or above  
- Maven  
- PostgreSQL  

### Start the application

```
mvn spring-boot:run
```

### Health check

```
GET http://localhost:8080/actuator/health
```

---

## Docker Support

Build the Docker image:

```
docker build -t financial-profile-api .
```

Run the image:

```
docker run -p 8080:8080 financial-profile-api
```

---

## Testing

Run tests:

```
mvn test
```

---

## Roadmap

- Add Swagger/OpenAPI documentation  
- Add JWT authentication  
- Add expense categories and analytics  
- Add multi-currency FX conversion  
- Add scheduling for automatic summary emails  
- Add CI pipeline with GitHub Actions  
- Add caching layer for market data  

---

## License

MIT License
