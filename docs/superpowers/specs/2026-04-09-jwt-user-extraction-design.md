# JWT User Extraction & Ownership Authorization Design

**Date:** 2026-04-09

## Context

Currently, all REST endpoints that operate on user-specific resources receive `userId` explicitly from the client — either as a request body field, query parameter, or path variable. This creates a security vulnerability: any authenticated user can pass any userId and access or create data for another user.

The goal is to extract the userId from the JWT token automatically, so users can only operate on their own resources. ADMIN users retain the ability to operate on any user's data by passing an explicit userId.

## Architecture

### Core Principle

- **MEMBER users**: `userId` is always extracted from the JWT `subject` claim. Any explicitly provided userId is ignored.
- **ADMIN users**: If an explicit `userId` is provided (in body or query param), that value is used. Otherwise, falls back to JWT userId.
- **Path variable userId access (GET /users/{id}, GET /summary/{userId}, etc.)**: MEMBER users get 404 if the path id doesn't match their JWT userId. ADMIN users can access any.
- **Resource ownership (PUT /assets/{id}, PUT/DELETE /summary-subscriptions/{id})**: MEMBER users get 404 if the resource's `userId` doesn't match their JWT userId. ADMIN users can modify any.

---

## Components

### 1. New: `SecurityUtils`

**File:** `src/main/java/com/financialhub/financialhubapi/infrastructure/security/SecurityUtils.java`

Static utility with three methods:
- `extractUserId(Jwt jwt)` → UUID from `jwt.getSubject()`
- `isAdmin(Jwt jwt)` → boolean, checks `jwt.getClaimAsString("role").equals("ADMIN")`
- `resolveUserId(Jwt jwt, UUID requestedUserId)` → if admin and requestedUserId != null, return requestedUserId; else return JWT userId

---

### 2. DTO Changes — `userId` becomes optional

MEMBER users don't send `userId` in request bodies. ADMIN users may optionally include it.

| DTO | Change |
|-----|--------|
| `AssetDtos.CreateAssetRequest` | `String userId` → `@Nullable String userId` |
| `ExpenseDtos.CreateExpenseRequest` | `String userId` → `@Nullable String userId` |
| `IncomeDtos.CreateIncomeRequest` | `String userId` → `@Nullable String userId` |
| `SummarySubscriptionDtos.CreateSubscriptionRequest` | Remove `@NotNull` from `UUID userId` → `@Nullable UUID userId` |

---

### 3. New Service Methods for Ownership Checks

**`AssetService`** — add:
```java
public Asset getAssetById(UUID assetId) {
    return assetRepository.findById(assetId)
            .orElseThrow(() -> new AssetNotFoundException(assetId));
}
```

**`SummarySubscriptionService`** — add:
```java
public SummarySubscription getSubscriptionById(UUID subscriptionId) {
    return subscriptionRepository.findById(subscriptionId)
            .orElseThrow(() -> new SubscriptionNotFoundException("Subscription not found: " + subscriptionId));
}
```

---

### 4. Controller Changes

All controllers add `@AuthenticationPrincipal Jwt jwt` as a parameter.

#### `AssetController`

- `POST /api/v1/assets`:
  ```java
  UUID userId = SecurityUtils.resolveUserId(jwt,
      request.userId() != null ? UUID.fromString(request.userId()) : null);
  ```
- `GET /api/v1/assets`:
  - Change `@RequestParam("userId") UUID userId` → `@RequestParam(required = false) UUID userId`
  - `UUID resolvedId = SecurityUtils.resolveUserId(jwt, userId);`
- `PUT /api/v1/assets/{id}`:
  - Fetch asset first via `assetService.getAssetById(id)`
  - If not admin and `asset.getUserId()` != JWT userId → throw `ResponseStatusException(NOT_FOUND)`
  - Then proceed with update

#### `ExpenseController` (same pattern as AssetController)

- `POST /api/v1/expenses`: resolve userId from JWT/body
- `GET /api/v1/expenses`: optional param, resolved via `resolveUserId`

#### `IncomeController` (same pattern)

- `POST /api/v1/incomes`: resolve userId
- `GET /api/v1/incomes`: optional param, resolved

#### `SummaryController`

- `GET /api/v1/summary/{userId}`:
  - If not admin and `userId` != JWT userId → `throw ResponseStatusException(NOT_FOUND)`
  - Otherwise proceed

#### `SummarySubscriptionController`

- `POST /api/v1/summary-subscriptions`: resolve userId from JWT/body
- `GET /api/v1/summary-subscriptions/user/{userId}`:
  - If not admin and `userId` != JWT userId → `throw ResponseStatusException(NOT_FOUND)`
- `PUT /api/v1/summary-subscriptions/{id}`:
  - Fetch subscription via `getSubscriptionById(id)`
  - If not admin and `subscription.getUserId()` != JWT userId → `throw ResponseStatusException(NOT_FOUND)`
- `DELETE /api/v1/summary-subscriptions/{id}`:
  - Same ownership check as PUT

#### `UserController`

- `GET /api/v1/users/{id}`:
  - If not admin and `id` != JWT userId → `throw ResponseStatusException(NOT_FOUND)`
- `PUT /api/v1/users/{id}`:
  - Same check

---

## Files to Create/Modify

| File | Action |
|------|--------|
| `infrastructure/security/SecurityUtils.java` | **Create** |
| `infrastructure/web/AssetController.java` | Modify |
| `infrastructure/web/ExpenseController.java` | Modify |
| `infrastructure/web/IncomeController.java` | Modify |
| `infrastructure/web/SummaryController.java` | Modify |
| `infrastructure/web/SummarySubscriptionController.java` | Modify |
| `infrastructure/web/UserController.java` | Modify |
| `infrastructure/web/dto/AssetDtos.java` | Modify |
| `infrastructure/web/dto/ExpenseDtos.java` | Modify |
| `infrastructure/web/dto/IncomeDtos.java` | Modify |
| `infrastructure/web/dto/SummarySubscriptionDtos.java` | Modify |
| `application/usecases/AssetService.java` | Add `getAssetById` |
| `application/usecases/SummarySubscriptionService.java` | Add `getSubscriptionById` |

---

## Verification

1. **MEMBER creates asset without userId** → 201 Created, asset belongs to JWT user
2. **MEMBER creates asset with someone else's userId in body** → 201 Created, asset still belongs to JWT user (userId ignored)
3. **MEMBER gets own assets** → 200, no query param needed
4. **MEMBER gets assets without userId param** → 200, returns own assets
5. **ADMIN gets assets with `?userId=otherUUID`** → 200, returns other user's assets
6. **MEMBER calls `GET /summary/{otherUserId}`** → 404
7. **ADMIN calls `GET /summary/{anyUserId}`** → 200
8. **MEMBER calls `PUT /assets/{id}` for someone else's asset** → 404
9. **ADMIN calls `PUT /assets/{id}` for any asset** → 200
10. **MEMBER calls `GET /users/{otherId}`** → 404
11. **MEMBER calls `PUT /users/{ownId}`** → 200
