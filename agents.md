# Financial Hub API - Service Agents

This document outlines the service agents (use cases) that power the Financial Hub API. Each agent encapsulates a specific domain of business logic within the application layer.

## Architecture

The Financial Hub API follows **Hexagonal Architecture** principles, where services act as use case orchestrators:

- **Domain Layer**: Pure business logic, entities, and validation rules
- **Application Layer**: Service agents that coordinate use cases
- **Infrastructure Layer**: Persistence, external integrations, and web controllers
- **Web Layer**: REST endpoints that delegate to services

---

## Service Agents

### 1. UserService

**Location**: `src/main/java/com/financialhub/financialhubapi/application/usecases/UserService.java`

**Responsibilities**:
- User registration with validation (email uniqueness, password strength, full name)
- User profile retrieval and updates
- Admin role promotion
- Password encoding and secure storage

**Key Methods**:
- `registerUser(String email, String fullName, String rawPassword)` - Register a new user with validation
- `getUser(UUID id)` - Retrieve a user by ID
- `updateUser(UUID id, String email, String fullName)` - Update user profile with email uniqueness check
- `promoteUserToAdmin(UUID userId)` - Grant admin privileges to a user
- `getAllUsers()` - Fetch all registered users

**Dependencies**:
- `UserRepository` - Persistence layer
- `List<UserRegistrationValidator>` - Pluggable validation chain
- `PasswordEncoder` - Spring Security password encoding

**Exceptions**:
- `UserNotFoundException` - User ID does not exist
- `EmailAlreadyExistsException` - Email already registered

---

### 2. AuthService

**Location**: `src/main/java/com/financialhub/financialhubapi/application/usecases/AuthService.java`

**Responsibilities**:
- User authentication (credential validation)
- Token generation and validation
- Login/logout operations
- Session management

**Key Methods**:
- `authenticate(String email, String password)` - Validate credentials and return auth token
- `validateToken(String token)` - Verify JWT or session token validity
- `logout(UUID userId)` - Clear user session/token

**Dependencies**:
- `UserRepository` - User lookup
- `PasswordEncoder` - Password comparison
- JWT or session token provider

---

### 3. AssetService

**Location**: `src/main/java/com/financialhub/financialhubapi/application/usecases/AssetService.java`

**Responsibilities**:
- CRUD operations for user assets (properties, stocks, crypto, cash, etc.)
- Asset valuation mode management (manual vs. market-based)
- Multi-currency asset handling

**Key Methods**:
- `createAsset(CreateAssetCommand cmd)` - Create a new asset for a user
- `getAssetsForUser(UUID userId)` - Retrieve all assets owned by a user
- `updateAsset(UUID assetId, UpdateAssetCommand cmd)` - Update asset details and valuation
- `deleteAsset(UUID assetId)` - Remove an asset

**Key Models**:
- `CreateAssetCommand` - DTO with userId, type, name, symbol, quantity, valuationMode, manualUnitValue, currency
- `UpdateAssetCommand` - Similar structure for updates
- `AssetType` - Enum: STOCK, CRYPTO, PROPERTY, CASH, etc.
- `ValuationMode` - Enum: MANUAL, MARKET_PRICE

**Dependencies**:
- `AssetRepository` - Asset persistence
- `MarketPricePort` - (indirect) For live valuations

**Exceptions**:
- `AssetNotFoundException` - Asset ID does not exist

---

### 4. IncomeService

**Location**: `src/main/java/com/financialhub/financialhubapi/application/usecases/IncomeService.java`

**Responsibilities**:
- CRUD operations for income streams (salary, freelance, investment returns, etc.)
- Income frequency and active period tracking
- Multi-currency income handling

**Key Methods**:
- `createIncome(UUID userId, String source, BigDecimal amount, String frequency)` - Record a new income source
- `getIncomeForUser(UUID userId)` - List all income streams for a user
- `updateIncome(UUID incomeId, ...)` - Update income details
- `deleteIncome(UUID incomeId)` - Remove an income record

**Key Models**:
- `Income` - Domain model with userId, source, amount, frequency (MONTHLY, ANNUAL, ONE_TIME), active period
- `Frequency` - Enum or entity determining payment schedule

**Dependencies**:
- `IncomeRepository` - Income persistence

---

### 5. ExpenseService

**Location**: `src/main/java/com/financialhub/financialhubapi/application/usecases/ExpenseService.java`

**Responsibilities**:
- CRUD operations for expense tracking (utilities, rent, groceries, subscriptions, etc.)
- Expense frequency and category management
- Multi-currency expense handling

**Key Methods**:
- `createExpense(UUID userId, String category, BigDecimal amount, String frequency)` - Record a new expense
- `getExpensesForUser(UUID userId)` - List all expenses for a user
- `updateExpense(UUID expenseId, ...)` - Update expense details
- `deleteExpense(UUID expenseId)` - Remove an expense record

**Key Models**:
- `Expense` - Domain model with userId, category, amount, frequency (MONTHLY, ANNUAL, ONE_TIME), active period
- `ExpenseCategory` - Enum: UTILITIES, RENT, FOOD, ENTERTAINMENT, SUBSCRIPTIONS, etc.

**Dependencies**:
- `ExpenseRepository` - Expense persistence

---

### 6. SummarySubscriptionService

**Location**: `src/main/java/com/financialhub/financialhubapi/application/usecases/SummarySubscriptionService.java`

**Responsibilities**:
- CRUD operations for user summary subscriptions (weekly or monthly email summaries)
- Tracks `nextSendAt` to determine when the next summary is due
- Consumed by `SummaryScheduler` to drive the notification pipeline

**Key Methods**:
- `createSubscription(UUID userId, String frequency, String currency)` - Subscribe a user to periodic summaries
- `getSubscription(UUID userId)` - Retrieve a user's active subscription
- `updateSubscription(UUID userId, ...)` - Change frequency or currency
- `deleteSubscription(UUID userId)` - Unsubscribe

**Dependencies**:
- `SummarySubscriptionRepository` - Subscription persistence

---

### 7. SummaryService

**Location**: `src/main/java/com/financialhub/financialhubapi/application/usecases/SummaryService.java`

**Responsibilities**:
- Calculate comprehensive financial summaries for users
- Aggregate asset valuations (with real-time market pricing)
- Compute monthly income, expenses, and savings metrics
- Calculate savings rate and financial health indicators

**Key Methods**:
- `getSummary(UUID userId, String currency)` - Generate complete financial summary in target currency

**Summary Output**:
```
Summary {
  userId: UUID,
  currency: String,
  totalAssetValue: BigDecimal,
  monthlyIncome: BigDecimal,
  monthlyExpenses: BigDecimal,
  monthlySavings: BigDecimal,
  savingsRate: BigDecimal (0.0 to 1.0),
  unpricedAssetCount: int
}
```

**Calculation Logic**:
1. Fetch all assets, incomes, and expenses for user
2. Calculate total asset value (convert to target currency):
   - Market-priced assets: fetch live price from `MarketPricePort`
   - Manually-valued assets: use stored manual value
3. Aggregate active incomes and expenses:
   - Convert to monthly amount based on frequency
   - Sum across all active records
   - Convert each to target currency
4. Calculate derived metrics:
   - `monthlySavings = monthlyIncome - monthlyExpenses`
   - `savingsRate = monthlySavings / monthlyIncome` (if income > 0)

**Dependencies**:
- `AssetRepository` - Fetch user assets
- `IncomeRepository` - Fetch user income streams
- `ExpenseRepository` - Fetch user expenses
- `MarketPricePort` - Query live asset prices (external integration)

**Key Models**:
- `Summary` - Output DTO with financial metrics
- `AssetValuationResult` - Intermediate result of asset calculation

---

## Python Services

### financial-summary-worker

**Location**: `../financial-summary-worker/` (sibling project, runs as a separate Docker container)

**Role**: Async email notification agent. Consumes financial summary snapshots published by the Java API, generates a rich HTML email with trend charts, and sends it to the user.

**Queue consumed**: `summary.notifications`  
**Dead-letter queue**: `summary.notifications.dlq`  
**Technology**: Python 3.12 · pika · Pydantic v2 · SQLite · matplotlib · Jinja2 · smtplib

**Pipeline per message**:
```
RabbitMQ message
  → Pydantic validation (security gate — rejects malformed messages → DLQ)
  → SQLite insert (persist snapshot for historical charts)
  → fetch history (last 12 snapshots for same user + currency)
  → matplotlib charts (savings rate trend + income vs. expenses)
  → Jinja2 HTML render (auto-escaped, no XSS)
  → SMTP send (STARTTLS enforced)
  → ACK
```

**At-least-once delivery**: message is ACKed only after the email is successfully sent. On any failure the message is NACKed with `requeue=False` and routed to the DLQ for manual inspection.

**Storage**: SQLite database at `DB_PATH` (default `/app/data/summaries.db`), mounted as a Docker volume so history survives container restarts. History is always filtered by `(user_id, currency)` — switching currency starts a fresh chart series.

**Key modules**:

| Module | Responsibility |
|--------|---------------|
| `worker/consumer.py` | RabbitMQ connection and message pipeline orchestration |
| `worker/models.py` | Pydantic `SummaryMessage` — camelCase JSON → snake_case, full field validation |
| `worker/repository.py` | SQLite `insert` + `get_history` (last N snapshots per user/currency) |
| `worker/charts.py` | matplotlib: savings rate line chart + income/expense bar chart → base64 PNG |
| `worker/renderer.py` | Jinja2 HTML template render with embedded charts |
| `worker/mailer.py` | smtplib STARTTLS email send, credentials from env only |

**Environment variables** (see `../financial-summary-worker/.env.example`):
- `RABBITMQ_HOST/PORT/USERNAME/PASSWORD`
- `SMTP_HOST/PORT/SMTP_USER/SMTP_PASSWORD/EMAIL_FROM`
- `DB_PATH`, `LOG_LEVEL`

**Security decisions**:
- PII (`user_email`, `user_name`) is never written to logs — only `user_id`
- Jinja2 `autoescape=True` prevents HTML injection in email body
- SMTP always uses STARTTLS — no plaintext fallback
- Docker container runs as non-root user (UID 1000)

---

## Service Integration Flow

```
API Request (synchronous)
    ↓
Controller (Web Layer)
    ↓
Service Agent (Application Layer)
    ↓
┌─────────────────────────┬──────────────────┬─────────────────┐
↓                         ↓                  ↓                 ↓
Domain Validation    Persistence        External Port    Business Logic
(Validators)         (Repositories)      (MarketPrice)    (Domain Models)


Scheduled notification pipeline (asynchronous)
    ↓
SummaryScheduler (every minute, checks nextSendAt)
    ↓
SummaryService.getSummary()  +  UserService.getUser()
    ↓
SummaryPublisher → RabbitMQ: summary.notifications
    ↓
financial-summary-worker (Python, separate container)
    ↓
SQLite history  →  matplotlib charts  →  Jinja2 HTML  →  SMTP email
```

### Example: Generate Financial Summary (on-demand)

1. Client requests: `GET /api/summary?userId={id}&currency=USD`
2. Controller delegates to `SummaryService.getSummary(userId, "USD")`
3. Service fetches data from repositories:
   - Assets from `AssetRepository`
   - Incomes from `IncomeRepository`
   - Expenses from `ExpenseRepository`
4. For market-valued assets, service calls `MarketPricePort` to fetch live prices
5. Service aggregates and calculates metrics
6. Returns `Summary` object to controller, which converts to JSON response

### Example: Scheduled Email Summary

1. `SummaryScheduler` runs every minute and queries subscriptions where `nextSendAt <= now`
2. For each due subscription: calls `SummaryService.getSummary()` + `UserService.getUser()`
3. `SummaryPublisher` serialises the result as JSON and publishes to `summary.notifications` via `summary.exchange`
4. `financial-summary-worker` (Python) consumes the message, stores a snapshot in SQLite, generates charts, renders HTML, and sends the email via SMTP
5. On success: ACK. On failure: NACK → message routed to `summary.notifications.dlq` for manual inspection

---

## Transactional Boundaries

Services use `@Transactional` annotation at the method level to ensure data consistency:
- `UserService.registerUser()` - Transactional (user + optional profile setup)
- `UserService.updateUser()` - Transactional (email uniqueness check + update)
- `SummaryService` - Read-only (queries only)

---

## Error Handling

All services throw domain-specific exceptions that are caught and converted to HTTP responses by the `GlobalExceptionHandler`:

- `UserNotFoundException`
- `EmailAlreadyExistsException`
- `AssetNotFoundException`
- `InvalidCredentialsException`
- `InvalidFullNameException`
- ... (other domain exceptions)

---

## Future Extensions

Potential service agents for future development:

- **BudgetService** - Set and track spending budgets
- **GoalService** - Track financial goals (e.g., save $10k, invest in crypto)
- **NotificationService** - Alert users on budget overruns, price milestones
- **AnalyticsService** - Provide trend analysis and spending patterns
- **email-dispatcher** - Extract SMTP sending from `financial-summary-worker` into a generic `email.outbox` consumer so other services can send emails without depending on the Python worker directly (~30 min refactor when needed)

---

## Testing Strategy

Each service agent has corresponding test coverage:

- **Unit Tests**: Test service methods with mocked dependencies
- **Integration Tests**: Test repositories and external integrations
- **Controller Tests**: End-to-end API validation

Located in: `src/test/java/com/financialhub/financialhubapi/`

