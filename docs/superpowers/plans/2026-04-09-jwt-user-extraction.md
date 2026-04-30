# JWT User Extraction & Ownership Authorization Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Replace explicit userId parameters with automatic extraction from the JWT token, while allowing ADMIN users to operate on any user's data.

**Architecture:** A new `SecurityUtils` static utility extracts the authenticated user's UUID from the JWT `subject` claim and provides an `isAdmin` check. Controllers inject `@AuthenticationPrincipal Jwt jwt`, resolve the effective userId via `SecurityUtils`, and enforce ownership for path-variable and resource-id endpoints. MEMBER users who attempt to access another user's resource receive 404.

**Tech Stack:** Spring Boot 3 / Spring Security OAuth2 Resource Server, `spring-security-test` for JWT mocking in `@WebMvcTest` tests, JUnit 5 + Mockito.

---

## File Map

| File | Action |
|------|--------|
| `pom.xml` | Add `spring-security-test` if not on classpath |
| `infrastructure/security/SecurityUtils.java` | **Create** |
| `infrastructure/web/dto/AssetDtos.java` | `userId` → `@Nullable String userId` |
| `infrastructure/web/dto/ExpenseDtos.java` | Same |
| `infrastructure/web/dto/IncomeDtos.java` | Same |
| `infrastructure/web/dto/SummarySubscriptionDtos.java` | Remove `@NotNull` from `userId` |
| `application/usecases/AssetService.java` | Add `getAssetById(UUID)` |
| `application/usecases/SummarySubscriptionService.java` | Add `getSubscriptionById(UUID)` |
| `infrastructure/web/AssetController.java` | Inject JWT, resolve userId, ownership check on PUT |
| `infrastructure/web/ExpenseController.java` | Inject JWT, resolve userId |
| `infrastructure/web/IncomeController.java` | Inject JWT, resolve userId |
| `infrastructure/web/SummaryController.java` | Inject JWT, ownership check on path var |
| `infrastructure/web/SummarySubscriptionController.java` | Inject JWT, resolve + ownership checks |
| `infrastructure/web/UserController.java` | Inject JWT, ownership check on GET/{id} and PUT/{id} |
| `web/AssetControllerTest.java` | Add JWT mocking, add ownership test cases |
| `web/ExpenseControllerTest.java` | Add JWT mocking, add ownership test cases |
| `web/IncomeControllerTest.java` | Add JWT mocking, add ownership test cases |
| `web/SummaryControllerTest.java` | Add JWT mocking, add ownership test cases |
| `web/SummarySubscriptionControllerTest.java` | Add JWT mocking, update userId-optional tests |
| `web/UserControllerTest.java` | Add JWT mocking, add ownership test cases |
| `infrastructure/security/SecurityUtilsTest.java` | **Create** |
| `application/usecases/AssetServiceTest.java` | Add `getAssetById` tests |
| `application/usecases/SummarySubscriptionServiceTest.java` | Add `getSubscriptionById` tests |

---

## Task 1: Add `spring-security-test` dependency

**Files:**
- Modify: `pom.xml`

- [ ] **Step 1: Check if `spring-security-test` is already on the classpath**

Run: `./mvnw dependency:tree -Dincludes=org.springframework.security:spring-security-test`

If output shows `spring-security-test`, skip to Task 2. Otherwise proceed.

- [ ] **Step 2: Add dependency to `pom.xml`**

After the closing `</dependency>` tag of `spring-boot-starter-webmvc-test`, add:

```xml
<dependency>
    <groupId>org.springframework.security</groupId>
    <artifactId>spring-security-test</artifactId>
    <scope>test</scope>
</dependency>
```

- [ ] **Step 3: Verify the project compiles**

Run: `./mvnw compile -q`
Expected: BUILD SUCCESS

- [ ] **Step 4: Commit**

```bash
git add pom.xml
git commit -m "test: add spring-security-test for JWT mocking in controller tests"
```

---

## Task 2: Create `SecurityUtils` with unit tests (TDD)

**Files:**
- Create: `src/main/java/com/financialhub/financialhubapi/infrastructure/security/SecurityUtils.java`
- Create: `src/test/java/com/financialhub/financialhubapi/infrastructure/security/SecurityUtilsTest.java`

- [ ] **Step 1: Write the failing tests**

Create `src/test/java/com/financialhub/financialhubapi/infrastructure/security/SecurityUtilsTest.java`:

```java
package com.financialhub.financialhubapi.infrastructure.security;

import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.Jwt;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class SecurityUtilsTest {

    private Jwt buildJwt(UUID subject, String role) {
        return Jwt.withTokenValue("token")
                .header("alg", "HS256")
                .subject(subject.toString())
                .claim("role", role)
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(3600))
                .build();
    }

    @Test
    void extractUserId_returnsUUIDFromSubject() {
        UUID userId = UUID.randomUUID();
        Jwt jwt = buildJwt(userId, "MEMBER");

        UUID result = SecurityUtils.extractUserId(jwt);

        assertThat(result).isEqualTo(userId);
    }

    @Test
    void isAdmin_returnsTrueForAdminRole() {
        Jwt jwt = buildJwt(UUID.randomUUID(), "ADMIN");
        assertThat(SecurityUtils.isAdmin(jwt)).isTrue();
    }

    @Test
    void isAdmin_returnsFalseForMemberRole() {
        Jwt jwt = buildJwt(UUID.randomUUID(), "MEMBER");
        assertThat(SecurityUtils.isAdmin(jwt)).isFalse();
    }

    @Test
    void resolveUserId_forMember_alwaysReturnsJwtUserId() {
        UUID jwtUserId = UUID.randomUUID();
        UUID requestedUserId = UUID.randomUUID();
        Jwt jwt = buildJwt(jwtUserId, "MEMBER");

        UUID result = SecurityUtils.resolveUserId(jwt, requestedUserId);

        assertThat(result).isEqualTo(jwtUserId);
    }

    @Test
    void resolveUserId_forMemberWithNullRequest_returnsJwtUserId() {
        UUID jwtUserId = UUID.randomUUID();
        Jwt jwt = buildJwt(jwtUserId, "MEMBER");

        UUID result = SecurityUtils.resolveUserId(jwt, null);

        assertThat(result).isEqualTo(jwtUserId);
    }

    @Test
    void resolveUserId_forAdmin_returnsRequestedUserIdWhenProvided() {
        UUID jwtUserId = UUID.randomUUID();
        UUID requestedUserId = UUID.randomUUID();
        Jwt jwt = buildJwt(jwtUserId, "ADMIN");

        UUID result = SecurityUtils.resolveUserId(jwt, requestedUserId);

        assertThat(result).isEqualTo(requestedUserId);
    }

    @Test
    void resolveUserId_forAdminWithNullRequest_returnsJwtUserId() {
        UUID jwtUserId = UUID.randomUUID();
        Jwt jwt = buildJwt(jwtUserId, "ADMIN");

        UUID result = SecurityUtils.resolveUserId(jwt, null);

        assertThat(result).isEqualTo(jwtUserId);
    }
}
```

- [ ] **Step 2: Run tests to verify they fail**

Run: `./mvnw test -pl . -Dtest=SecurityUtilsTest -q`
Expected: FAIL — `SecurityUtils` class does not exist

- [ ] **Step 3: Create `SecurityUtils`**

Create `src/main/java/com/financialhub/financialhubapi/infrastructure/security/SecurityUtils.java`:

```java
package com.financialhub.financialhubapi.infrastructure.security;

import org.springframework.security.oauth2.jwt.Jwt;

import java.util.UUID;

public final class SecurityUtils {

    private SecurityUtils() {}

    public static UUID extractUserId(Jwt jwt) {
        return UUID.fromString(jwt.getSubject());
    }

    public static boolean isAdmin(Jwt jwt) {
        return "ADMIN".equals(jwt.getClaimAsString("role"));
    }

    /**
     * Returns requestedUserId if the caller is ADMIN and requestedUserId is non-null.
     * Otherwise returns the userId from the JWT token.
     */
    public static UUID resolveUserId(Jwt jwt, UUID requestedUserId) {
        if (isAdmin(jwt) && requestedUserId != null) {
            return requestedUserId;
        }
        return extractUserId(jwt);
    }
}
```

- [ ] **Step 4: Run tests to verify they pass**

Run: `./mvnw test -pl . -Dtest=SecurityUtilsTest -q`
Expected: BUILD SUCCESS, all 7 tests passing

- [ ] **Step 5: Commit**

```bash
git add src/main/java/com/financialhub/financialhubapi/infrastructure/security/SecurityUtils.java \
        src/test/java/com/financialhub/financialhubapi/infrastructure/security/SecurityUtilsTest.java
git commit -m "feat: add SecurityUtils for JWT user extraction and admin role check"
```

---

## Task 3: Make `userId` nullable in DTOs

**Files:**
- Modify: `src/main/java/com/financialhub/financialhubapi/infrastructure/web/dto/AssetDtos.java`
- Modify: `src/main/java/com/financialhub/financialhubapi/infrastructure/web/dto/ExpenseDtos.java`
- Modify: `src/main/java/com/financialhub/financialhubapi/infrastructure/web/dto/IncomeDtos.java`
- Modify: `src/main/java/com/financialhub/financialhubapi/infrastructure/web/dto/SummarySubscriptionDtos.java`

> No TDD here — these are data containers with no logic. Compilation is the verification.

- [ ] **Step 1: Update `AssetDtos.java`**

In `CreateAssetRequest`, add `@Nullable` import and annotation:

```java
package com.financialhub.financialhubapi.infrastructure.web.dto;

import com.financialhub.financialhubapi.domain.model.Asset;
import org.springframework.lang.Nullable;

import java.math.BigDecimal;

public class AssetDtos {
    public record CreateAssetRequest(
            @Nullable String userId,
            String type,
            String name,
            String symbol,
            BigDecimal quantity,
            String valuationMode,
            BigDecimal manualUnitValue,
            String currency
    ) {}

    public record AssetResponse(
            String id,
            String userId,
            String type,
            String name,
            String symbol,
            BigDecimal quantity,
            String valuationMode,
            BigDecimal manualUnitValue,
            String currency
    ) {
        public static AssetResponse from(Asset asset) {
            return new AssetResponse(
                    asset.getId().toString(),
                    asset.getUserId().toString(),
                    asset.getType().name(),
                    asset.getName(),
                    asset.getSymbol(),
                    asset.getQuantity(),
                    asset.getValuationMode().name(),
                    asset.getManualUnitValue(),
                    asset.getCurrency()
            );
        }
    }

    public record UpdateAssetRequest(
            String name,
            String symbol,
            BigDecimal quantity,
            String valuationMode,
            BigDecimal manualUnitValue,
            String currency
    ) {}
}
```

- [ ] **Step 2: Update `ExpenseDtos.java`**

Replace the `CreateExpenseRequest` record:

```java
package com.financialhub.financialhubapi.infrastructure.web.dto;

import com.financialhub.financialhubapi.domain.model.Expense;
import com.financialhub.financialhubapi.domain.model.ExpenseFrequency;
import org.springframework.lang.Nullable;

import java.math.BigDecimal;
import java.time.LocalDate;

public class ExpenseDtos {
    public record CreateExpenseRequest(
            @Nullable String userId,
            String category,
            String description,
            ExpenseFrequency frequency,
            BigDecimal amount,
            String currency,
            LocalDate startsAt,
            LocalDate endsAt
    ) {}

    public record ExpenseResponse(
            String id,
            String userId,
            String category,
            String description,
            ExpenseFrequency frequency,
            BigDecimal amount,
            String currency,
            LocalDate startsAt,
            LocalDate endsAt
    ) {
        public static ExpenseResponse from(Expense expense) {
            return new ExpenseResponse(
                    expense.getId().toString(),
                    expense.getUserId().toString(),
                    expense.getCategory().name(),
                    expense.getDescription(),
                    expense.getFrequency(),
                    expense.getAmount(),
                    expense.getCurrency(),
                    expense.getStartsAt(),
                    expense.getEndsAt()
            );
        }
    }
}
```

- [ ] **Step 3: Update `IncomeDtos.java`**

Replace the `CreateIncomeRequest` record:

```java
package com.financialhub.financialhubapi.infrastructure.web.dto;

import com.financialhub.financialhubapi.domain.model.Income;
import org.springframework.lang.Nullable;

import java.math.BigDecimal;
import java.time.LocalDate;

public class IncomeDtos {
    public record CreateIncomeRequest(
            @Nullable String userId,
            String source,
            String frequency,
            BigDecimal amount,
            String currency,
            LocalDate startsAt,
            LocalDate endsAt
    ) {}

    public record IncomeResponse(
            String id,
            String userId,
            String source,
            String frequency,
            BigDecimal amount,
            String currency,
            LocalDate startsAt,
            LocalDate endsAt
    ) {
        public static IncomeResponse from(Income income) {
            return new IncomeResponse(
                    income.getId().toString(),
                    income.getUserId().toString(),
                    income.getSource(),
                    income.getFrequency().name(),
                    income.getAmount(),
                    income.getCurrency(),
                    income.getStartsAt(),
                    income.getEndsAt()
            );
        }
    }
}
```

- [ ] **Step 4: Update `SummarySubscriptionDtos.java`**

Remove `@NotNull` from `userId` field:

```java
package com.financialhub.financialhubapi.infrastructure.web.dto;

import com.financialhub.financialhubapi.domain.model.SummarySubscription;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import org.springframework.lang.Nullable;

import java.util.UUID;

public class SummarySubscriptionDtos {

    public record CreateSubscriptionRequest(
            @Nullable UUID userId,
            @NotNull SummarySubscription.Frequency frequency,
            @NotBlank @Pattern(regexp = "^[A-Z]{3}$", message = "must be a 3-letter currency code") String currency
    ) {}

    public record UpdateSubscriptionRequest(
            @NotNull SummarySubscription.Frequency frequency
    ) {}

    public record SubscriptionResponse(
            String id,
            String userId,
            String frequency,
            String currency,
            String status,
            String nextSendAt,
            String createdAt
    ) {
        public static SubscriptionResponse from(SummarySubscription s) {
            return new SubscriptionResponse(
                    s.getId().toString(),
                    s.getUserId().toString(),
                    s.getFrequency().name(),
                    s.getCurrency(),
                    s.getStatus().name(),
                    s.getNextSendAt().toString(),
                    s.getCreatedAt().toString()
            );
        }
    }
}
```

- [ ] **Step 5: Verify compilation**

Run: `./mvnw compile -q`
Expected: BUILD SUCCESS

- [ ] **Step 6: Commit**

```bash
git add src/main/java/com/financialhub/financialhubapi/infrastructure/web/dto/
git commit -m "feat: make userId optional in create request DTOs"
```

---

## Task 4: Add `getAssetById` to `AssetService` (TDD)

**Files:**
- Modify: `src/main/java/com/financialhub/financialhubapi/application/usecases/AssetService.java`
- Modify: `src/test/java/com/financialhub/financialhubapi/application/usecases/AssetServiceTest.java`

- [ ] **Step 1: Write failing test**

Add this test to `AssetServiceTest.java` (inside the existing class, after the last test):

```java
@Test
void getAssetById_returnsAssetWhenFound() {
    UUID assetId = UUID.randomUUID();
    UUID userId = UUID.randomUUID();
    Asset asset = new Asset(assetId, userId, AssetType.STOCK, "Apple", "AAPL",
            new BigDecimal("10"), ValuationMode.MARKET, null, "AUD", Instant.now());

    when(assetRepository.findById(assetId)).thenReturn(Optional.of(asset));

    Asset result = assetService.getAssetById(assetId);

    assertThat(result.getId()).isEqualTo(assetId);
}

@Test
void getAssetById_throwsAssetNotFoundWhenMissing() {
    UUID assetId = UUID.randomUUID();
    when(assetRepository.findById(assetId)).thenReturn(Optional.empty());

    assertThrows(AssetNotFoundException.class, () -> assetService.getAssetById(assetId));
}
```

- [ ] **Step 2: Run to verify failure**

Run: `./mvnw test -pl . -Dtest=AssetServiceTest -q`
Expected: FAIL — `getAssetById` method does not exist

- [ ] **Step 3: Add `getAssetById` to `AssetService`**

In `AssetService.java`, add after `getAssetsForUser`:

```java
public Asset getAssetById(UUID assetId) {
    return assetRepository.findById(assetId)
            .orElseThrow(() -> new AssetNotFoundException(assetId));
}
```

- [ ] **Step 4: Run tests to verify they pass**

Run: `./mvnw test -pl . -Dtest=AssetServiceTest -q`
Expected: BUILD SUCCESS

- [ ] **Step 5: Commit**

```bash
git add src/main/java/com/financialhub/financialhubapi/application/usecases/AssetService.java \
        src/test/java/com/financialhub/financialhubapi/application/usecases/AssetServiceTest.java
git commit -m "feat: add getAssetById to AssetService for ownership checks"
```

---

## Task 5: Add `getSubscriptionById` to `SummarySubscriptionService` (TDD)

**Files:**
- Modify: `src/main/java/com/financialhub/financialhubapi/application/usecases/SummarySubscriptionService.java`
- Modify: `src/test/java/com/financialhub/financialhubapi/application/usecases/SummarySubscriptionServiceTest.java`

- [ ] **Step 1: Write failing test**

Add to `SummarySubscriptionServiceTest.java` (read the file first to understand current structure, then add after last test):

```java
@Test
void getSubscriptionById_returnsSubscriptionWhenFound() {
    UUID subscriptionId = UUID.randomUUID();
    UUID userId = UUID.randomUUID();
    SummarySubscription subscription = new SummarySubscription(
            subscriptionId, userId, SummarySubscription.Frequency.WEEKLY, "AUD",
            SummarySubscription.Status.ACTIVE, Instant.now().plusSeconds(3600), Instant.now()
    );

    when(subscriptionRepository.findById(subscriptionId)).thenReturn(Optional.of(subscription));

    SummarySubscription result = subscriptionService.getSubscriptionById(subscriptionId);

    assertThat(result.getId()).isEqualTo(subscriptionId);
}

@Test
void getSubscriptionById_throwsWhenNotFound() {
    UUID subscriptionId = UUID.randomUUID();
    when(subscriptionRepository.findById(subscriptionId)).thenReturn(Optional.empty());

    assertThrows(SubscriptionNotFoundException.class,
            () -> subscriptionService.getSubscriptionById(subscriptionId));
}
```

- [ ] **Step 2: Run to verify failure**

Run: `./mvnw test -pl . -Dtest=SummarySubscriptionServiceTest -q`
Expected: FAIL — `getSubscriptionById` does not exist

- [ ] **Step 3: Add `getSubscriptionById` to `SummarySubscriptionService`**

In `SummarySubscriptionService.java`, add as a public method (before `getActiveSubscription`):

```java
public SummarySubscription getSubscriptionById(UUID subscriptionId) {
    return subscriptionRepository.findById(subscriptionId)
            .orElseThrow(() -> new SubscriptionNotFoundException(
                    "Subscription not found with id: " + subscriptionId));
}
```

- [ ] **Step 4: Run tests to verify they pass**

Run: `./mvnw test -pl . -Dtest=SummarySubscriptionServiceTest -q`
Expected: BUILD SUCCESS

- [ ] **Step 5: Commit**

```bash
git add src/main/java/com/financialhub/financialhubapi/application/usecases/SummarySubscriptionService.java \
        src/test/java/com/financialhub/financialhubapi/application/usecases/SummarySubscriptionServiceTest.java
git commit -m "feat: add getSubscriptionById to SummarySubscriptionService for ownership checks"
```

---

## Task 6: Update `AssetController` + tests (TDD)

**Files:**
- Modify: `src/main/java/com/financialhub/financialhubapi/infrastructure/web/AssetController.java`
- Modify: `src/test/java/com/financialhub/financialhubapi/web/AssetControllerTest.java`

### Changes summary
- `POST /assets`: extract userId from JWT (ADMIN can override via body)
- `GET /assets`: `userId` query param becomes optional; ADMIN can override
- `PUT /assets/{id}`: fetch asset, check ownership (MEMBER → 404 if mismatch)

- [ ] **Step 1: Write failing tests**

Replace the content of `AssetControllerTest.java` with:

```java
package com.financialhub.financialhubapi.web;

import com.financialhub.financialhubapi.domain.exceptions.AssetNotFoundException;
import com.financialhub.financialhubapi.infrastructure.web.AssetController;
import com.financialhub.financialhubapi.infrastructure.web.dto.AssetDtos;
import com.financialhub.financialhubapi.application.usecases.AssetService;
import com.financialhub.financialhubapi.domain.model.Asset;
import com.financialhub.financialhubapi.domain.model.AssetType;
import com.financialhub.financialhubapi.domain.model.valuation.ValuationMode;
import tools.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = AssetController.class)
class AssetControllerTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;
    @MockitoBean AssetService assetService;

    private Asset sampleAsset(UUID assetId, UUID userId) {
        return new Asset(assetId, userId, AssetType.CRYPTO, "Bitcoin", "BTCUSDT",
                new BigDecimal("0.5"), ValuationMode.MARKET, null, "USD", Instant.now());
    }

    // ── POST /api/v1/assets ──────────────────────────────────────────────────

    @Test
    void createAsset_memberExtractsUserIdFromJwt() throws Exception {
        UUID userId = UUID.randomUUID();
        UUID assetId = UUID.randomUUID();

        when(assetService.createAsset(any())).thenReturn(sampleAsset(assetId, userId));

        // MEMBER sends no userId in body — extracted from JWT
        mockMvc.perform(post("/api/v1/assets")
                        .with(jwt().claim("sub", userId.toString()).claim("role", "MEMBER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {
                              "type": "CRYPTO",
                              "name": "Bitcoin",
                              "symbol": "BTCUSDT",
                              "quantity": 0.5,
                              "valuationMode": "MARKET",
                              "currency": "USD"
                            }
                            """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.userId").value(userId.toString()));
    }

    @Test
    void createAsset_memberIgnoresUserIdInBody() throws Exception {
        UUID jwtUserId = UUID.randomUUID();
        UUID otherUserId = UUID.randomUUID();
        UUID assetId = UUID.randomUUID();

        // Service receives the JWT userId, not the body's userId
        when(assetService.createAsset(any())).thenReturn(sampleAsset(assetId, jwtUserId));

        mockMvc.perform(post("/api/v1/assets")
                        .with(jwt().claim("sub", jwtUserId.toString()).claim("role", "MEMBER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {
                              "userId": "%s",
                              "type": "CRYPTO",
                              "name": "Bitcoin",
                              "symbol": "BTCUSDT",
                              "quantity": 0.5,
                              "valuationMode": "MARKET",
                              "currency": "USD"
                            }
                            """.formatted(otherUserId)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.userId").value(jwtUserId.toString()));
    }

    @Test
    void createAsset_adminCanCreateForAnotherUser() throws Exception {
        UUID adminId = UUID.randomUUID();
        UUID targetUserId = UUID.randomUUID();
        UUID assetId = UUID.randomUUID();

        when(assetService.createAsset(any())).thenReturn(sampleAsset(assetId, targetUserId));

        mockMvc.perform(post("/api/v1/assets")
                        .with(jwt().claim("sub", adminId.toString()).claim("role", "ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {
                              "userId": "%s",
                              "type": "CRYPTO",
                              "name": "Bitcoin",
                              "symbol": "BTCUSDT",
                              "quantity": 0.5,
                              "valuationMode": "MARKET",
                              "currency": "USD"
                            }
                            """.formatted(targetUserId)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.userId").value(targetUserId.toString()));
    }

    // ── GET /api/v1/assets ───────────────────────────────────────────────────

    @Test
    void listAssets_memberGetsOwnAssetsWithoutQueryParam() throws Exception {
        UUID userId = UUID.randomUUID();
        Asset asset = sampleAsset(UUID.randomUUID(), userId);

        when(assetService.getAssetsForUser(userId)).thenReturn(List.of(asset));

        mockMvc.perform(get("/api/v1/assets")
                        .with(jwt().claim("sub", userId.toString()).claim("role", "MEMBER")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].userId").value(userId.toString()));
    }

    @Test
    void listAssets_adminCanQueryOtherUserWithParam() throws Exception {
        UUID adminId = UUID.randomUUID();
        UUID targetUserId = UUID.randomUUID();
        Asset asset = sampleAsset(UUID.randomUUID(), targetUserId);

        when(assetService.getAssetsForUser(targetUserId)).thenReturn(List.of(asset));

        mockMvc.perform(get("/api/v1/assets")
                        .param("userId", targetUserId.toString())
                        .with(jwt().claim("sub", adminId.toString()).claim("role", "ADMIN")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].userId").value(targetUserId.toString()));
    }

    // ── PUT /api/v1/assets/{id} ──────────────────────────────────────────────

    @Test
    void updateAsset_memberCanUpdateOwnAsset() throws Exception {
        UUID userId = UUID.randomUUID();
        UUID assetId = UUID.randomUUID();
        Asset existing = sampleAsset(assetId, userId);
        Asset updated = new Asset(assetId, userId, AssetType.CRYPTO, "Bitcoin Updated", "BTC",
                new BigDecimal("1.0"), ValuationMode.MARKET, null, "AUD", Instant.now());

        when(assetService.getAssetById(assetId)).thenReturn(existing);
        when(assetService.updateAsset(eq(assetId), any())).thenReturn(updated);

        mockMvc.perform(put("/api/v1/assets/" + assetId)
                        .with(jwt().claim("sub", userId.toString()).claim("role", "MEMBER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new AssetDtos.UpdateAssetRequest(
                                "Bitcoin Updated", "BTC", new BigDecimal("1.0"), "MARKET", null, "AUD"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Bitcoin Updated"));
    }

    @Test
    void updateAsset_memberCannotUpdateOtherUsersAsset() throws Exception {
        UUID userId = UUID.randomUUID();
        UUID otherUserId = UUID.randomUUID();
        UUID assetId = UUID.randomUUID();
        Asset existingOtherUser = sampleAsset(assetId, otherUserId);

        when(assetService.getAssetById(assetId)).thenReturn(existingOtherUser);

        mockMvc.perform(put("/api/v1/assets/" + assetId)
                        .with(jwt().claim("sub", userId.toString()).claim("role", "MEMBER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new AssetDtos.UpdateAssetRequest(
                                "Bitcoin", "BTC", new BigDecimal("1.0"), "MARKET", null, "AUD"))))
                .andExpect(status().isNotFound());
    }

    @Test
    void updateAsset_adminCanUpdateAnyAsset() throws Exception {
        UUID adminId = UUID.randomUUID();
        UUID targetUserId = UUID.randomUUID();
        UUID assetId = UUID.randomUUID();
        Asset existing = sampleAsset(assetId, targetUserId);
        Asset updated = new Asset(assetId, targetUserId, AssetType.CRYPTO, "Bitcoin Updated", "BTC",
                new BigDecimal("1.0"), ValuationMode.MARKET, null, "AUD", Instant.now());

        when(assetService.getAssetById(assetId)).thenReturn(existing);
        when(assetService.updateAsset(eq(assetId), any())).thenReturn(updated);

        mockMvc.perform(put("/api/v1/assets/" + assetId)
                        .with(jwt().claim("sub", adminId.toString()).claim("role", "ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new AssetDtos.UpdateAssetRequest(
                                "Bitcoin Updated", "BTC", new BigDecimal("1.0"), "MARKET", null, "AUD"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Bitcoin Updated"));
    }

    @Test
    void updateAsset_returns404WhenAssetNotFound() throws Exception {
        UUID userId = UUID.randomUUID();
        UUID assetId = UUID.randomUUID();

        when(assetService.getAssetById(assetId)).thenThrow(new AssetNotFoundException(assetId));

        mockMvc.perform(put("/api/v1/assets/" + assetId)
                        .with(jwt().claim("sub", userId.toString()).claim("role", "MEMBER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new AssetDtos.UpdateAssetRequest(
                                "Bitcoin", "BTC", new BigDecimal("1.0"), "MARKET", null, "AUD"))))
                .andExpect(status().isNotFound());
    }
}
```

- [ ] **Step 2: Run tests to verify they fail**

Run: `./mvnw test -pl . -Dtest=AssetControllerTest -q`
Expected: FAIL — controller doesn't use JWT yet

- [ ] **Step 3: Update `AssetController`**

Replace the content of `AssetController.java` with:

```java
package com.financialhub.financialhubapi.infrastructure.web;

import com.financialhub.financialhubapi.application.usecases.AssetService;
import com.financialhub.financialhubapi.application.usecases.AssetService.CreateAssetCommand;
import com.financialhub.financialhubapi.domain.model.Asset;
import com.financialhub.financialhubapi.infrastructure.security.SecurityUtils;
import com.financialhub.financialhubapi.infrastructure.web.dto.AssetDtos.*;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.net.URI;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/assets")
public class AssetController {

    private final AssetService assetService;

    public AssetController(AssetService assetService) {
        this.assetService = assetService;
    }

    @PostMapping
    public ResponseEntity<AssetResponse> createAsset(
            @AuthenticationPrincipal Jwt jwt,
            @RequestBody CreateAssetRequest request) {

        UUID userId = SecurityUtils.resolveUserId(jwt,
                request.userId() != null ? UUID.fromString(request.userId()) : null);

        CreateAssetCommand cmd = new CreateAssetCommand(
                userId,
                request.type(),
                request.name(),
                request.symbol(),
                request.quantity(),
                request.valuationMode(),
                request.manualUnitValue(),
                request.currency()
        );

        Asset asset = assetService.createAsset(cmd);
        AssetResponse body = AssetResponse.from(asset);
        URI location = URI.create("/api/v1/assets/" + asset.getId());
        return ResponseEntity.created(location).body(body);
    }

    @GetMapping
    public List<AssetResponse> listAssets(
            @AuthenticationPrincipal Jwt jwt,
            @RequestParam(required = false) UUID userId) {

        UUID resolvedId = SecurityUtils.resolveUserId(jwt, userId);
        return assetService.getAssetsForUser(resolvedId)
                .stream()
                .map(AssetResponse::from)
                .toList();
    }

    @PutMapping("/{id}")
    public ResponseEntity<AssetResponse> updateAsset(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID id,
            @RequestBody UpdateAssetRequest request) {

        Asset existing = assetService.getAssetById(id);
        if (!SecurityUtils.isAdmin(jwt) && !existing.getUserId().equals(SecurityUtils.extractUserId(jwt))) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }

        AssetService.UpdateAssetCommand cmd = new AssetService.UpdateAssetCommand(
                request.name(),
                request.symbol(),
                request.quantity(),
                request.valuationMode(),
                request.manualUnitValue(),
                request.currency()
        );

        Asset updated = assetService.updateAsset(id, cmd);
        return ResponseEntity.ok(AssetResponse.from(updated));
    }
}
```

- [ ] **Step 4: Run tests to verify they pass**

Run: `./mvnw test -pl . -Dtest=AssetControllerTest -q`
Expected: BUILD SUCCESS

- [ ] **Step 5: Commit**

```bash
git add src/main/java/com/financialhub/financialhubapi/infrastructure/web/AssetController.java \
        src/test/java/com/financialhub/financialhubapi/web/AssetControllerTest.java
git commit -m "feat: extract userId from JWT in AssetController, enforce ownership on PUT"
```

---

## Task 7: Update `ExpenseController` + tests (TDD)

**Files:**
- Modify: `src/main/java/com/financialhub/financialhubapi/infrastructure/web/ExpenseController.java`
- Modify: `src/test/java/com/financialhub/financialhubapi/web/ExpenseControllerTest.java`

- [ ] **Step 1: Write failing tests**

Replace content of `ExpenseControllerTest.java`:

```java
package com.financialhub.financialhubapi.web;

import com.financialhub.financialhubapi.infrastructure.web.ExpenseController;
import tools.jackson.databind.ObjectMapper;
import com.financialhub.financialhubapi.application.usecases.ExpenseService;
import com.financialhub.financialhubapi.domain.model.Expense;
import com.financialhub.financialhubapi.domain.model.ExpenseCategory;
import com.financialhub.financialhubapi.domain.model.ExpenseFrequency;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = ExpenseController.class)
class ExpenseControllerTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;
    @MockitoBean ExpenseService expenseService;

    private Expense sampleExpense(UUID userId) {
        return new Expense(UUID.randomUUID(), userId, ExpenseCategory.RENT, "Rent payment",
                ExpenseFrequency.MONTHLY, new BigDecimal("2400.00"), "AUD",
                LocalDate.parse("2026-01-01"), null, Instant.now());
    }

    @Test
    void createExpense_memberExtractsUserIdFromJwt() throws Exception {
        UUID userId = UUID.randomUUID();
        Expense expense = sampleExpense(userId);
        when(expenseService.createExpense(any())).thenReturn(expense);

        mockMvc.perform(post("/api/v1/expenses")
                        .with(jwt().claim("sub", userId.toString()).claim("role", "MEMBER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {
                              "category": "RENT",
                              "description": "Rent payment",
                              "frequency": "MONTHLY",
                              "amount": 2400.00,
                              "currency": "AUD",
                              "startsAt": "2026-01-01"
                            }
                            """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.userId").value(userId.toString()))
                .andExpect(jsonPath("$.category").value("RENT"));
    }

    @Test
    void createExpense_adminCanCreateForAnotherUser() throws Exception {
        UUID adminId = UUID.randomUUID();
        UUID targetUserId = UUID.randomUUID();
        Expense expense = sampleExpense(targetUserId);
        when(expenseService.createExpense(any())).thenReturn(expense);

        mockMvc.perform(post("/api/v1/expenses")
                        .with(jwt().claim("sub", adminId.toString()).claim("role", "ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {
                              "userId": "%s",
                              "category": "RENT",
                              "description": "Rent payment",
                              "frequency": "MONTHLY",
                              "amount": 2400.00,
                              "currency": "AUD",
                              "startsAt": "2026-01-01"
                            }
                            """.formatted(targetUserId)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.userId").value(targetUserId.toString()));
    }

    @Test
    void listExpenses_memberGetsOwnExpensesWithoutQueryParam() throws Exception {
        UUID userId = UUID.randomUUID();
        Expense expense = sampleExpense(userId);
        when(expenseService.getExpensesForUser(userId)).thenReturn(List.of(expense));

        mockMvc.perform(get("/api/v1/expenses")
                        .with(jwt().claim("sub", userId.toString()).claim("role", "MEMBER")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].userId").value(userId.toString()));
    }

    @Test
    void listExpenses_adminCanQueryOtherUser() throws Exception {
        UUID adminId = UUID.randomUUID();
        UUID targetUserId = UUID.randomUUID();
        Expense expense = sampleExpense(targetUserId);
        when(expenseService.getExpensesForUser(targetUserId)).thenReturn(List.of(expense));

        mockMvc.perform(get("/api/v1/expenses")
                        .param("userId", targetUserId.toString())
                        .with(jwt().claim("sub", adminId.toString()).claim("role", "ADMIN")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].userId").value(targetUserId.toString()));
    }
}
```

- [ ] **Step 2: Run to verify failure**

Run: `./mvnw test -pl . -Dtest=ExpenseControllerTest -q`
Expected: FAIL

- [ ] **Step 3: Update `ExpenseController`**

Replace `ExpenseController.java` content:

```java
package com.financialhub.financialhubapi.infrastructure.web;

import com.financialhub.financialhubapi.application.usecases.ExpenseService;
import com.financialhub.financialhubapi.infrastructure.security.SecurityUtils;
import com.financialhub.financialhubapi.infrastructure.web.dto.ExpenseDtos.*;
import com.financialhub.financialhubapi.domain.model.Expense;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/expenses")
public class ExpenseController {

    private final ExpenseService expenseService;

    public ExpenseController(ExpenseService expenseService) {
        this.expenseService = expenseService;
    }

    @PostMapping
    public ResponseEntity<ExpenseResponse> createExpense(
            @AuthenticationPrincipal Jwt jwt,
            @RequestBody CreateExpenseRequest request) {

        UUID userId = SecurityUtils.resolveUserId(jwt,
                request.userId() != null ? UUID.fromString(request.userId()) : null);

        ExpenseService.CreateExpenseCommand cmd = new ExpenseService.CreateExpenseCommand(
                userId,
                request.category(),
                request.description(),
                request.frequency(),
                request.amount(),
                request.currency(),
                request.startsAt(),
                request.endsAt()
        );

        Expense expense = expenseService.createExpense(cmd);
        URI location = URI.create("/api/v1/expenses/" + expense.getId());
        return ResponseEntity.created(location).body(ExpenseResponse.from(expense));
    }

    @GetMapping
    public List<ExpenseResponse> listExpenses(
            @AuthenticationPrincipal Jwt jwt,
            @RequestParam(required = false) UUID userId) {

        UUID resolvedId = SecurityUtils.resolveUserId(jwt, userId);
        return expenseService.getExpensesForUser(resolvedId)
                .stream()
                .map(ExpenseResponse::from)
                .toList();
    }
}
```

- [ ] **Step 4: Run tests to verify they pass**

Run: `./mvnw test -pl . -Dtest=ExpenseControllerTest -q`
Expected: BUILD SUCCESS

- [ ] **Step 5: Commit**

```bash
git add src/main/java/com/financialhub/financialhubapi/infrastructure/web/ExpenseController.java \
        src/test/java/com/financialhub/financialhubapi/web/ExpenseControllerTest.java
git commit -m "feat: extract userId from JWT in ExpenseController"
```

---

## Task 8: Update `IncomeController` + tests (TDD)

**Files:**
- Modify: `src/main/java/com/financialhub/financialhubapi/infrastructure/web/IncomeController.java`
- Modify: `src/test/java/com/financialhub/financialhubapi/web/IncomeControllerTest.java`

- [ ] **Step 1: Write failing tests**

Replace content of `IncomeControllerTest.java`:

```java
package com.financialhub.financialhubapi.web;

import com.financialhub.financialhubapi.infrastructure.web.IncomeController;
import tools.jackson.databind.ObjectMapper;
import com.financialhub.financialhubapi.application.usecases.IncomeService;
import com.financialhub.financialhubapi.domain.model.Income;
import com.financialhub.financialhubapi.domain.model.IncomeFrequency;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = IncomeController.class)
class IncomeControllerTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;
    @MockitoBean IncomeService incomeService;

    private Income sampleIncome(UUID userId) {
        return new Income(UUID.randomUUID(), userId, "Salary", IncomeFrequency.MONTHLY,
                new BigDecimal("9000.00"), "AUD", LocalDate.parse("2026-01-01"), null, Instant.now());
    }

    @Test
    void createIncome_memberExtractsUserIdFromJwt() throws Exception {
        UUID userId = UUID.randomUUID();
        Income income = sampleIncome(userId);
        when(incomeService.createIncome(any())).thenReturn(income);

        mockMvc.perform(post("/api/v1/incomes")
                        .with(jwt().claim("sub", userId.toString()).claim("role", "MEMBER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {
                              "source": "Salary",
                              "frequency": "MONTHLY",
                              "amount": 9000.00,
                              "currency": "AUD",
                              "startsAt": "2026-01-01"
                            }
                            """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.userId").value(userId.toString()))
                .andExpect(jsonPath("$.source").value("Salary"));
    }

    @Test
    void createIncome_adminCanCreateForAnotherUser() throws Exception {
        UUID adminId = UUID.randomUUID();
        UUID targetUserId = UUID.randomUUID();
        Income income = sampleIncome(targetUserId);
        when(incomeService.createIncome(any())).thenReturn(income);

        mockMvc.perform(post("/api/v1/incomes")
                        .with(jwt().claim("sub", adminId.toString()).claim("role", "ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {
                              "userId": "%s",
                              "source": "Salary",
                              "frequency": "MONTHLY",
                              "amount": 9000.00,
                              "currency": "AUD",
                              "startsAt": "2026-01-01"
                            }
                            """.formatted(targetUserId)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.userId").value(targetUserId.toString()));
    }

    @Test
    void listIncomes_memberGetsOwnIncomesWithoutQueryParam() throws Exception {
        UUID userId = UUID.randomUUID();
        Income income = sampleIncome(userId);
        when(incomeService.getIncomesForUser(userId)).thenReturn(List.of(income));

        mockMvc.perform(get("/api/v1/incomes")
                        .with(jwt().claim("sub", userId.toString()).claim("role", "MEMBER")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].userId").value(userId.toString()));
    }

    @Test
    void listIncomes_adminCanQueryOtherUser() throws Exception {
        UUID adminId = UUID.randomUUID();
        UUID targetUserId = UUID.randomUUID();
        Income income = sampleIncome(targetUserId);
        when(incomeService.getIncomesForUser(targetUserId)).thenReturn(List.of(income));

        mockMvc.perform(get("/api/v1/incomes")
                        .param("userId", targetUserId.toString())
                        .with(jwt().claim("sub", adminId.toString()).claim("role", "ADMIN")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].userId").value(targetUserId.toString()));
    }
}
```

- [ ] **Step 2: Run to verify failure**

Run: `./mvnw test -pl . -Dtest=IncomeControllerTest -q`
Expected: FAIL

- [ ] **Step 3: Update `IncomeController`**

Replace `IncomeController.java` content:

```java
package com.financialhub.financialhubapi.infrastructure.web;

import com.financialhub.financialhubapi.application.usecases.IncomeService;
import com.financialhub.financialhubapi.infrastructure.security.SecurityUtils;
import com.financialhub.financialhubapi.infrastructure.web.dto.IncomeDtos.*;
import com.financialhub.financialhubapi.domain.model.Income;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/incomes")
public class IncomeController {

    private final IncomeService incomeService;

    public IncomeController(IncomeService incomeService) {
        this.incomeService = incomeService;
    }

    @PostMapping
    public ResponseEntity<IncomeResponse> createIncome(
            @AuthenticationPrincipal Jwt jwt,
            @RequestBody CreateIncomeRequest request) {

        UUID userId = SecurityUtils.resolveUserId(jwt,
                request.userId() != null ? UUID.fromString(request.userId()) : null);

        IncomeService.CreateIncomeCommand cmd = new IncomeService.CreateIncomeCommand(
                userId,
                request.source(),
                request.frequency(),
                request.amount(),
                request.currency(),
                request.startsAt(),
                request.endsAt()
        );

        Income income = incomeService.createIncome(cmd);
        URI location = URI.create("/api/v1/incomes/" + income.getId());
        return ResponseEntity.created(location).body(IncomeResponse.from(income));
    }

    @GetMapping
    public List<IncomeResponse> listIncomes(
            @AuthenticationPrincipal Jwt jwt,
            @RequestParam(required = false) UUID userId) {

        UUID resolvedId = SecurityUtils.resolveUserId(jwt, userId);
        return incomeService.getIncomesForUser(resolvedId)
                .stream()
                .map(IncomeResponse::from)
                .toList();
    }
}
```

- [ ] **Step 4: Run tests to verify they pass**

Run: `./mvnw test -pl . -Dtest=IncomeControllerTest -q`
Expected: BUILD SUCCESS

- [ ] **Step 5: Commit**

```bash
git add src/main/java/com/financialhub/financialhubapi/infrastructure/web/IncomeController.java \
        src/test/java/com/financialhub/financialhubapi/web/IncomeControllerTest.java
git commit -m "feat: extract userId from JWT in IncomeController"
```

---

## Task 9: Update `SummaryController` + tests (TDD)

**Files:**
- Modify: `src/main/java/com/financialhub/financialhubapi/infrastructure/web/SummaryController.java`
- Modify: `src/test/java/com/financialhub/financialhubapi/web/SummaryControllerTest.java`

- [ ] **Step 1: Write failing tests**

Replace content of `SummaryControllerTest.java`:

```java
package com.financialhub.financialhubapi.web;

import com.financialhub.financialhubapi.application.usecases.SummaryService;
import com.financialhub.financialhubapi.infrastructure.web.SummaryController;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.UUID;

import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = SummaryController.class)
class SummaryControllerTest {

    @Autowired MockMvc mockMvc;
    @MockitoBean SummaryService summaryService;

    private SummaryService.Summary summary(UUID userId, String currency) {
        return new SummaryService.Summary(userId, currency, new BigDecimal("150000"),
                new BigDecimal("5000"), new BigDecimal("3000"), new BigDecimal("2000"),
                new BigDecimal("0.40"), 0);
    }

    @Test
    void getSummary_memberCanAccessOwnSummary() throws Exception {
        UUID userId = UUID.randomUUID();
        when(summaryService.getSummary(userId, "AUD")).thenReturn(summary(userId, "AUD"));

        mockMvc.perform(get("/api/v1/summary/{userId}", userId)
                        .with(jwt().claim("sub", userId.toString()).claim("role", "MEMBER")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.currency").value("AUD"))
                .andExpect(jsonPath("$.totalAssetsValue").value(150000));
    }

    @Test
    void getSummary_memberCannotAccessOtherUserSummary() throws Exception {
        UUID userId = UUID.randomUUID();
        UUID otherUserId = UUID.randomUUID();

        mockMvc.perform(get("/api/v1/summary/{userId}", otherUserId)
                        .with(jwt().claim("sub", userId.toString()).claim("role", "MEMBER")))
                .andExpect(status().isNotFound());
    }

    @Test
    void getSummary_adminCanAccessAnySummary() throws Exception {
        UUID adminId = UUID.randomUUID();
        UUID targetUserId = UUID.randomUUID();
        when(summaryService.getSummary(targetUserId, "AUD")).thenReturn(summary(targetUserId, "AUD"));

        mockMvc.perform(get("/api/v1/summary/{userId}", targetUserId)
                        .with(jwt().claim("sub", adminId.toString()).claim("role", "ADMIN")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.currency").value("AUD"));
    }

    @Test
    void getSummary_withExplicitCurrency_passesItToService() throws Exception {
        UUID userId = UUID.randomUUID();
        when(summaryService.getSummary(userId, "USD")).thenReturn(summary(userId, "USD"));

        mockMvc.perform(get("/api/v1/summary/{userId}", userId)
                        .param("currency", "USD")
                        .with(jwt().claim("sub", userId.toString()).claim("role", "MEMBER")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.currency").value("USD"));
    }
}
```

- [ ] **Step 2: Run to verify failure**

Run: `./mvnw test -pl . -Dtest=SummaryControllerTest -q`
Expected: FAIL

- [ ] **Step 3: Update `SummaryController`**

Replace `SummaryController.java` content:

```java
package com.financialhub.financialhubapi.infrastructure.web;

import com.financialhub.financialhubapi.application.usecases.SummaryService;
import com.financialhub.financialhubapi.infrastructure.security.SecurityUtils;
import com.financialhub.financialhubapi.infrastructure.web.dto.SummaryDtos.SummaryResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/summary")
public class SummaryController {

    private final SummaryService summaryService;

    public SummaryController(SummaryService summaryService) {
        this.summaryService = summaryService;
    }

    @GetMapping("/{userId}")
    public ResponseEntity<SummaryResponse> getSummary(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID userId,
            @RequestParam(defaultValue = "AUD") String currency) {

        if (!SecurityUtils.isAdmin(jwt) && !userId.equals(SecurityUtils.extractUserId(jwt))) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }

        SummaryService.Summary summary = summaryService.getSummary(userId, currency);
        return ResponseEntity.ok(SummaryResponse.from(summary));
    }
}
```

- [ ] **Step 4: Run tests to verify they pass**

Run: `./mvnw test -pl . -Dtest=SummaryControllerTest -q`
Expected: BUILD SUCCESS

- [ ] **Step 5: Commit**

```bash
git add src/main/java/com/financialhub/financialhubapi/infrastructure/web/SummaryController.java \
        src/test/java/com/financialhub/financialhubapi/web/SummaryControllerTest.java
git commit -m "feat: enforce ownership in SummaryController, MEMBER gets 404 for other users"
```

---

## Task 10: Update `SummarySubscriptionController` + tests (TDD)

**Files:**
- Modify: `src/main/java/com/financialhub/financialhubapi/infrastructure/web/SummarySubscriptionController.java`
- Modify: `src/test/java/com/financialhub/financialhubapi/web/SummarySubscriptionControllerTest.java`

- [ ] **Step 1: Write failing tests**

Replace content of `SummarySubscriptionControllerTest.java`:

```java
package com.financialhub.financialhubapi.web;

import tools.jackson.databind.ObjectMapper;
import com.financialhub.financialhubapi.application.usecases.SummarySubscriptionService;
import com.financialhub.financialhubapi.domain.exceptions.SubscriptionNotFoundException;
import com.financialhub.financialhubapi.domain.exceptions.UserNotFoundException;
import com.financialhub.financialhubapi.domain.model.SummarySubscription;
import com.financialhub.financialhubapi.infrastructure.web.SummarySubscriptionController;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.UUID;

import static com.financialhub.financialhubapi.domain.model.SummarySubscription.Frequency.MONTHLY;
import static com.financialhub.financialhubapi.domain.model.SummarySubscription.Frequency.WEEKLY;
import static com.financialhub.financialhubapi.domain.model.SummarySubscription.Status.ACTIVE;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = SummarySubscriptionController.class)
class SummarySubscriptionControllerTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;
    @MockitoBean SummarySubscriptionService subscriptionService;

    private UUID userId;
    private UUID subscriptionId;
    private SummarySubscription activeSubscription;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        subscriptionId = UUID.randomUUID();
        activeSubscription = new SummarySubscription(
                subscriptionId, userId, WEEKLY, "AUD",
                ACTIVE, Instant.now().plusSeconds(3600), Instant.now());
    }

    // ── POST /api/v1/summary-subscriptions ───────────────────────────────────

    @Test
    void create_memberExtractsUserIdFromJwt() throws Exception {
        when(subscriptionService.create(userId, WEEKLY, "AUD")).thenReturn(activeSubscription);

        mockMvc.perform(post("/api/v1/summary-subscriptions")
                        .with(jwt().claim("sub", userId.toString()).claim("role", "MEMBER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            { "frequency": "WEEKLY", "currency": "AUD" }
                            """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.userId").value(userId.toString()))
                .andExpect(jsonPath("$.frequency").value("WEEKLY"))
                .andExpect(jsonPath("$.status").value("ACTIVE"));
    }

    @Test
    void create_adminCanCreateForAnotherUser() throws Exception {
        UUID adminId = UUID.randomUUID();
        UUID targetUserId = UUID.randomUUID();
        SummarySubscription sub = new SummarySubscription(
                subscriptionId, targetUserId, WEEKLY, "AUD",
                ACTIVE, Instant.now().plusSeconds(3600), Instant.now());
        when(subscriptionService.create(targetUserId, WEEKLY, "AUD")).thenReturn(sub);

        mockMvc.perform(post("/api/v1/summary-subscriptions")
                        .with(jwt().claim("sub", adminId.toString()).claim("role", "ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            { "userId": "%s", "frequency": "WEEKLY", "currency": "AUD" }
                            """.formatted(targetUserId)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.userId").value(targetUserId.toString()));
    }

    @Test
    void create_whenUserNotFound_returns404() throws Exception {
        when(subscriptionService.create(any(), any(), any()))
                .thenThrow(new UserNotFoundException("User not found with id: " + userId));

        mockMvc.perform(post("/api/v1/summary-subscriptions")
                        .with(jwt().claim("sub", userId.toString()).claim("role", "MEMBER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            { "frequency": "WEEKLY", "currency": "AUD" }
                            """))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("USER_NOT_FOUND"));
    }

    @Test
    void create_whenInvalidCurrencyFormat_returns400() throws Exception {
        mockMvc.perform(post("/api/v1/summary-subscriptions")
                        .with(jwt().claim("sub", userId.toString()).claim("role", "MEMBER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            { "frequency": "WEEKLY", "currency": "australian_dollar" }
                            """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.fieldErrors.currency").exists());
    }

    // ── GET /api/v1/summary-subscriptions/user/{userId} ──────────────────────

    @Test
    void getActiveForUser_memberCanAccessOwnSubscription() throws Exception {
        when(subscriptionService.getActiveForUser(userId)).thenReturn(activeSubscription);

        mockMvc.perform(get("/api/v1/summary-subscriptions/user/{userId}", userId)
                        .with(jwt().claim("sub", userId.toString()).claim("role", "MEMBER")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(userId.toString()))
                .andExpect(jsonPath("$.status").value("ACTIVE"));
    }

    @Test
    void getActiveForUser_memberCannotAccessOtherUserSubscription() throws Exception {
        UUID otherUserId = UUID.randomUUID();

        mockMvc.perform(get("/api/v1/summary-subscriptions/user/{userId}", otherUserId)
                        .with(jwt().claim("sub", userId.toString()).claim("role", "MEMBER")))
                .andExpect(status().isNotFound());
    }

    @Test
    void getActiveForUser_adminCanAccessAnySubscription() throws Exception {
        UUID adminId = UUID.randomUUID();
        UUID targetUserId = UUID.randomUUID();
        SummarySubscription sub = new SummarySubscription(
                subscriptionId, targetUserId, WEEKLY, "AUD",
                ACTIVE, Instant.now().plusSeconds(3600), Instant.now());
        when(subscriptionService.getActiveForUser(targetUserId)).thenReturn(sub);

        mockMvc.perform(get("/api/v1/summary-subscriptions/user/{userId}", targetUserId)
                        .with(jwt().claim("sub", adminId.toString()).claim("role", "ADMIN")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(targetUserId.toString()));
    }

    @Test
    void getActiveForUser_whenNoneActive_returns404() throws Exception {
        when(subscriptionService.getActiveForUser(userId))
                .thenThrow(new SubscriptionNotFoundException("No active subscription for user: " + userId));

        mockMvc.perform(get("/api/v1/summary-subscriptions/user/{userId}", userId)
                        .with(jwt().claim("sub", userId.toString()).claim("role", "MEMBER")))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("SUBSCRIPTION_NOT_FOUND"));
    }

    // ── PUT /api/v1/summary-subscriptions/{id} ────────────────────────────────

    @Test
    void updateFrequency_memberCanUpdateOwnSubscription() throws Exception {
        SummarySubscription updated = new SummarySubscription(
                subscriptionId, userId, MONTHLY, "AUD",
                ACTIVE, Instant.now().plusSeconds(3600), Instant.now());
        when(subscriptionService.getSubscriptionById(subscriptionId)).thenReturn(activeSubscription);
        when(subscriptionService.updateFrequency(subscriptionId, MONTHLY)).thenReturn(updated);

        mockMvc.perform(put("/api/v1/summary-subscriptions/{id}", subscriptionId)
                        .with(jwt().claim("sub", userId.toString()).claim("role", "MEMBER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            { "frequency": "MONTHLY" }
                            """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.frequency").value("MONTHLY"));
    }

    @Test
    void updateFrequency_memberCannotUpdateOtherUsersSubscription() throws Exception {
        UUID otherUserId = UUID.randomUUID();
        SummarySubscription otherUserSub = new SummarySubscription(
                subscriptionId, otherUserId, WEEKLY, "AUD",
                ACTIVE, Instant.now().plusSeconds(3600), Instant.now());
        when(subscriptionService.getSubscriptionById(subscriptionId)).thenReturn(otherUserSub);

        mockMvc.perform(put("/api/v1/summary-subscriptions/{id}", subscriptionId)
                        .with(jwt().claim("sub", userId.toString()).claim("role", "MEMBER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            { "frequency": "MONTHLY" }
                            """))
                .andExpect(status().isNotFound());
    }

    @Test
    void updateFrequency_adminCanUpdateAnySubscription() throws Exception {
        UUID adminId = UUID.randomUUID();
        UUID targetUserId = UUID.randomUUID();
        SummarySubscription targetSub = new SummarySubscription(
                subscriptionId, targetUserId, WEEKLY, "AUD",
                ACTIVE, Instant.now().plusSeconds(3600), Instant.now());
        SummarySubscription updated = new SummarySubscription(
                subscriptionId, targetUserId, MONTHLY, "AUD",
                ACTIVE, Instant.now().plusSeconds(3600), Instant.now());
        when(subscriptionService.getSubscriptionById(subscriptionId)).thenReturn(targetSub);
        when(subscriptionService.updateFrequency(subscriptionId, MONTHLY)).thenReturn(updated);

        mockMvc.perform(put("/api/v1/summary-subscriptions/{id}", subscriptionId)
                        .with(jwt().claim("sub", adminId.toString()).claim("role", "ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            { "frequency": "MONTHLY" }
                            """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.frequency").value("MONTHLY"));
    }

    @Test
    void updateFrequency_whenNotFound_returns404() throws Exception {
        when(subscriptionService.getSubscriptionById(subscriptionId))
                .thenThrow(new SubscriptionNotFoundException("Not found: " + subscriptionId));

        mockMvc.perform(put("/api/v1/summary-subscriptions/{id}", subscriptionId)
                        .with(jwt().claim("sub", userId.toString()).claim("role", "MEMBER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            { "frequency": "MONTHLY" }
                            """))
                .andExpect(status().isNotFound());
    }

    // ── DELETE /api/v1/summary-subscriptions/{id} ────────────────────────────

    @Test
    void cancel_memberCanCancelOwnSubscription() throws Exception {
        when(subscriptionService.getSubscriptionById(subscriptionId)).thenReturn(activeSubscription);
        doNothing().when(subscriptionService).cancel(subscriptionId);

        mockMvc.perform(delete("/api/v1/summary-subscriptions/{id}", subscriptionId)
                        .with(jwt().claim("sub", userId.toString()).claim("role", "MEMBER")))
                .andExpect(status().isNoContent());

        verify(subscriptionService).cancel(subscriptionId);
    }

    @Test
    void cancel_memberCannotCancelOtherUsersSubscription() throws Exception {
        UUID otherUserId = UUID.randomUUID();
        SummarySubscription otherUserSub = new SummarySubscription(
                subscriptionId, otherUserId, WEEKLY, "AUD",
                ACTIVE, Instant.now().plusSeconds(3600), Instant.now());
        when(subscriptionService.getSubscriptionById(subscriptionId)).thenReturn(otherUserSub);

        mockMvc.perform(delete("/api/v1/summary-subscriptions/{id}", subscriptionId)
                        .with(jwt().claim("sub", userId.toString()).claim("role", "MEMBER")))
                .andExpect(status().isNotFound());
    }

    @Test
    void cancel_adminCanCancelAnySubscription() throws Exception {
        UUID adminId = UUID.randomUUID();
        UUID targetUserId = UUID.randomUUID();
        SummarySubscription targetSub = new SummarySubscription(
                subscriptionId, targetUserId, WEEKLY, "AUD",
                ACTIVE, Instant.now().plusSeconds(3600), Instant.now());
        when(subscriptionService.getSubscriptionById(subscriptionId)).thenReturn(targetSub);
        doNothing().when(subscriptionService).cancel(subscriptionId);

        mockMvc.perform(delete("/api/v1/summary-subscriptions/{id}", subscriptionId)
                        .with(jwt().claim("sub", adminId.toString()).claim("role", "ADMIN")))
                .andExpect(status().isNoContent());

        verify(subscriptionService).cancel(subscriptionId);
    }

    @Test
    void cancel_whenNotFound_returns404() throws Exception {
        when(subscriptionService.getSubscriptionById(subscriptionId))
                .thenThrow(new SubscriptionNotFoundException("Not found: " + subscriptionId));

        mockMvc.perform(delete("/api/v1/summary-subscriptions/{id}", subscriptionId)
                        .with(jwt().claim("sub", userId.toString()).claim("role", "MEMBER")))
                .andExpect(status().isNotFound());
    }
}
```

- [ ] **Step 2: Run to verify failure**

Run: `./mvnw test -pl . -Dtest=SummarySubscriptionControllerTest -q`
Expected: FAIL

- [ ] **Step 3: Update `SummarySubscriptionController`**

Replace `SummarySubscriptionController.java` content:

```java
package com.financialhub.financialhubapi.infrastructure.web;

import com.financialhub.financialhubapi.application.usecases.SummarySubscriptionService;
import com.financialhub.financialhubapi.domain.model.SummarySubscription;
import com.financialhub.financialhubapi.infrastructure.security.SecurityUtils;
import com.financialhub.financialhubapi.infrastructure.web.dto.SummarySubscriptionDtos.CreateSubscriptionRequest;
import com.financialhub.financialhubapi.infrastructure.web.dto.SummarySubscriptionDtos.SubscriptionResponse;
import com.financialhub.financialhubapi.infrastructure.web.dto.SummarySubscriptionDtos.UpdateSubscriptionRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/summary-subscriptions")
public class SummarySubscriptionController {

    private final SummarySubscriptionService subscriptionService;

    public SummarySubscriptionController(SummarySubscriptionService subscriptionService) {
        this.subscriptionService = subscriptionService;
    }

    @PostMapping
    public ResponseEntity<SubscriptionResponse> create(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody CreateSubscriptionRequest request) {

        UUID userId = SecurityUtils.resolveUserId(jwt, request.userId());
        SummarySubscription subscription = subscriptionService.create(userId, request.frequency(), request.currency());
        return ResponseEntity.status(HttpStatus.CREATED).body(SubscriptionResponse.from(subscription));
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<SubscriptionResponse> getActiveForUser(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID userId) {

        if (!SecurityUtils.isAdmin(jwt) && !userId.equals(SecurityUtils.extractUserId(jwt))) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }

        SummarySubscription subscription = subscriptionService.getActiveForUser(userId);
        return ResponseEntity.ok(SubscriptionResponse.from(subscription));
    }

    @PutMapping("/{id}")
    public ResponseEntity<SubscriptionResponse> updateFrequency(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID id,
            @Valid @RequestBody UpdateSubscriptionRequest request) {

        SummarySubscription existing = subscriptionService.getSubscriptionById(id);
        if (!SecurityUtils.isAdmin(jwt) && !existing.getUserId().equals(SecurityUtils.extractUserId(jwt))) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }

        SummarySubscription subscription = subscriptionService.updateFrequency(id, request.frequency());
        return ResponseEntity.ok(SubscriptionResponse.from(subscription));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> cancel(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID id) {

        SummarySubscription existing = subscriptionService.getSubscriptionById(id);
        if (!SecurityUtils.isAdmin(jwt) && !existing.getUserId().equals(SecurityUtils.extractUserId(jwt))) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }

        subscriptionService.cancel(id);
        return ResponseEntity.noContent().build();
    }
}
```

- [ ] **Step 4: Run tests to verify they pass**

Run: `./mvnw test -pl . -Dtest=SummarySubscriptionControllerTest -q`
Expected: BUILD SUCCESS

- [ ] **Step 5: Commit**

```bash
git add src/main/java/com/financialhub/financialhubapi/infrastructure/web/SummarySubscriptionController.java \
        src/test/java/com/financialhub/financialhubapi/web/SummarySubscriptionControllerTest.java
git commit -m "feat: extract userId from JWT in SummarySubscriptionController, enforce ownership"
```

---

## Task 11: Update `UserController` + tests (TDD)

**Files:**
- Modify: `src/main/java/com/financialhub/financialhubapi/infrastructure/web/UserController.java`
- Modify: `src/test/java/com/financialhub/financialhubapi/web/UserControllerTest.java`

### Changes summary
- `GET /users/{id}`: MEMBER gets 404 if `id` != JWT userId
- `PUT /users/{id}`: same ownership check
- `GET /users` (ADMIN only), `POST /users` (public), `POST /users/{id}/promote` (ADMIN only): unchanged

- [ ] **Step 1: Write failing tests**

Add the following tests to `UserControllerTest.java` (append inside the existing class, before the closing brace). Keep all existing tests — they will be updated to include JWT:

Replace the full content of `UserControllerTest.java`:

```java
package com.financialhub.financialhubapi.web;

import com.financialhub.financialhubapi.application.usecases.UserService;
import com.financialhub.financialhubapi.domain.exceptions.EmailAlreadyExistsException;
import com.financialhub.financialhubapi.domain.exceptions.InvalidFullNameException;
import com.financialhub.financialhubapi.domain.exceptions.WeakPasswordException;
import com.financialhub.financialhubapi.domain.model.Role;
import com.financialhub.financialhubapi.domain.model.User;
import com.financialhub.financialhubapi.infrastructure.web.UserController;
import com.financialhub.financialhubapi.infrastructure.web.dto.UserDtos.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = UserController.class)
class UserControllerTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;
    @MockitoBean UserService userService;

    private User sampleUser(UUID id, String email, String fullName) {
        return new User(id, email, fullName, "hashed", Instant.now(), Role.MEMBER);
    }

    // ── POST /api/v1/users (public — no JWT needed) ──────────────────────────

    @Test
    void createUser_returns201AndBody() throws Exception {
        var payload = new CreateUserRequest("alice@example.com", "Alice Doe", "secret123");
        var user = sampleUser(UUID.randomUUID(), "alice@example.com", "Alice Doe");

        when(userService.registerUser(anyString(), anyString(), anyString())).thenReturn(user);

        mockMvc.perform(post("/api/v1/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isCreated())
                .andExpect(header().exists("Location"))
                .andExpect(jsonPath("$.email").value("alice@example.com"))
                .andExpect(jsonPath("$.fullName").value("Alice Doe"))
                .andExpect(jsonPath("$.role").value("MEMBER"));
    }

    @Test
    void createUser_whenEmailAlreadyExists_returns409() throws Exception {
        var payload = new CreateUserRequest("alice@example.com", "Alice Doe", "secret123");
        when(userService.registerUser(anyString(), anyString(), anyString()))
                .thenThrow(new EmailAlreadyExistsException("Email already exists"));

        mockMvc.perform(post("/api/v1/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("EMAIL_ALREADY_EXISTS"));
    }

    @Test
    void createUser_whenInvalidFullNameException_returns400() throws Exception {
        var payload = new CreateUserRequest("alice@example.com", "Alice3", "secret123");
        when(userService.registerUser(anyString(), anyString(), anyString()))
                .thenThrow(new InvalidFullNameException("Full name cannot contain numbers"));

        mockMvc.perform(post("/api/v1/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_FULL_NAME"));
    }

    @Test
    void createUser_whenWeakPasswordException_returns400() throws Exception {
        var payload = new CreateUserRequest("alice@example.com", "Alice", "secret123");
        when(userService.registerUser(anyString(), anyString(), anyString()))
                .thenThrow(new WeakPasswordException("Password must be at least 8 characters long and include " +
                        "at least 2 digits, 1 uppercase letter and 1 special character"));

        mockMvc.perform(post("/api/v1/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("WEAK_PASSWORD"));
    }

    @Test
    void whenInvalidRequest_thenReturns400AndFieldErrors() throws Exception {
        var body = """
                {
                  "email": "not-an-email",
                  "fullName": "A",
                  "password": "123"
                }
                """;

        mockMvc.perform(post("/api/v1/users")
                        .contentType("application/json")
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.fieldErrors.email").exists())
                .andExpect(jsonPath("$.fieldErrors.fullName").exists())
                .andExpect(jsonPath("$.fieldErrors.password").exists());
    }

    // ── GET /api/v1/users (ADMIN only) ───────────────────────────────────────

    @Test
    void getAllUsers_adminCanListAllUsers() throws Exception {
        UUID adminId = UUID.randomUUID();
        User user = sampleUser(UUID.randomUUID(), "test@example.com", "John Doe");
        when(userService.getAllUsers()).thenReturn(List.of(user));

        mockMvc.perform(get("/api/v1/users")
                        .with(jwt().claim("sub", adminId.toString()).claim("role", "ADMIN")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].email").value("test@example.com"));
    }

    // ── GET /api/v1/users/{id} ───────────────────────────────────────────────

    @Test
    void getUser_memberCanAccessOwnProfile() throws Exception {
        UUID userId = UUID.randomUUID();
        User user = sampleUser(userId, "test@example.com", "John Doe");
        when(userService.getUser(userId)).thenReturn(user);

        mockMvc.perform(get("/api/v1/users/{id}", userId)
                        .with(jwt().claim("sub", userId.toString()).claim("role", "MEMBER")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(userId.toString()))
                .andExpect(jsonPath("$.role").value("MEMBER"));
    }

    @Test
    void getUser_memberCannotAccessOtherUserProfile() throws Exception {
        UUID userId = UUID.randomUUID();
        UUID otherUserId = UUID.randomUUID();

        mockMvc.perform(get("/api/v1/users/{id}", otherUserId)
                        .with(jwt().claim("sub", userId.toString()).claim("role", "MEMBER")))
                .andExpect(status().isNotFound());
    }

    @Test
    void getUser_adminCanAccessAnyUserProfile() throws Exception {
        UUID adminId = UUID.randomUUID();
        UUID targetUserId = UUID.randomUUID();
        User user = sampleUser(targetUserId, "other@example.com", "Other User");
        when(userService.getUser(targetUserId)).thenReturn(user);

        mockMvc.perform(get("/api/v1/users/{id}", targetUserId)
                        .with(jwt().claim("sub", adminId.toString()).claim("role", "ADMIN")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(targetUserId.toString()));
    }

    // ── PUT /api/v1/users/{id} ───────────────────────────────────────────────

    @Test
    void updateUser_memberCanUpdateOwnProfile() throws Exception {
        UUID userId = UUID.randomUUID();
        User updated = sampleUser(userId, "new@example.com", "New Name");
        when(userService.updateUser(eq(userId), eq("new@example.com"), eq("New Name"))).thenReturn(updated);

        mockMvc.perform(put("/api/v1/users/{id}", userId)
                        .with(jwt().claim("sub", userId.toString()).claim("role", "MEMBER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new UpdateUserRequest("new@example.com", "New Name"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("new@example.com"));
    }

    @Test
    void updateUser_memberCannotUpdateOtherUserProfile() throws Exception {
        UUID userId = UUID.randomUUID();
        UUID otherUserId = UUID.randomUUID();

        mockMvc.perform(put("/api/v1/users/{id}", otherUserId)
                        .with(jwt().claim("sub", userId.toString()).claim("role", "MEMBER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new UpdateUserRequest("x@x.com", "X"))))
                .andExpect(status().isNotFound());
    }

    @Test
    void updateUser_adminCanUpdateAnyProfile() throws Exception {
        UUID adminId = UUID.randomUUID();
        UUID targetUserId = UUID.randomUUID();
        User updated = sampleUser(targetUserId, "new@example.com", "New Name");
        when(userService.updateUser(eq(targetUserId), eq("new@example.com"), eq("New Name"))).thenReturn(updated);

        mockMvc.perform(put("/api/v1/users/{id}", targetUserId)
                        .with(jwt().claim("sub", adminId.toString()).claim("role", "ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new UpdateUserRequest("new@example.com", "New Name"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(targetUserId.toString()));
    }

    // test-only record to build the JSON request body
    record CreateUserRequest(String email, String fullName, String password) {}
}
```

- [ ] **Step 2: Run to verify failure**

Run: `./mvnw test -pl . -Dtest=UserControllerTest -q`
Expected: FAIL

- [ ] **Step 3: Update `UserController`**

Replace `UserController.java` content:

```java
package com.financialhub.financialhubapi.infrastructure.web;

import com.financialhub.financialhubapi.application.usecases.UserService;
import com.financialhub.financialhubapi.domain.model.User;
import com.financialhub.financialhubapi.infrastructure.security.SecurityUtils;
import com.financialhub.financialhubapi.infrastructure.web.dto.UserDtos.UpdateUserRequest;
import com.financialhub.financialhubapi.infrastructure.web.dto.UserDtos.CreateUserRequest;
import com.financialhub.financialhubapi.infrastructure.web.dto.UserDtos.UserResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.net.URI;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping
    public ResponseEntity<UserResponse> createUser(@Valid @RequestBody CreateUserRequest request) {
        User user = userService.registerUser(request.email(), request.fullName(), request.password());
        UserResponse body = new UserResponse(user.getId(), user.getEmail(), user.getFullName(), user.getRole(), user.getCreatedAt());
        URI location = URI.create("/api/v1/users/" + user.getId());
        return ResponseEntity.created(location).body(body);
    }

    @PreAuthorize("hasAuthority('ADMIN')")
    @GetMapping
    public ResponseEntity<List<UserResponse>> getAllUsers() {
        List<UserResponse> response = userService.getAllUsers().stream()
                .map(UserResponse::from)
                .toList();
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    public UserResponse getUser(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID id) {

        if (!SecurityUtils.isAdmin(jwt) && !id.equals(SecurityUtils.extractUserId(jwt))) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }

        User user = userService.getUser(id);
        return new UserResponse(user.getId(), user.getEmail(), user.getFullName(), user.getRole(), user.getCreatedAt());
    }

    @PutMapping("/{id}")
    public ResponseEntity<UserResponse> updateUser(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID id,
            @Valid @RequestBody UpdateUserRequest request) {

        if (!SecurityUtils.isAdmin(jwt) && !id.equals(SecurityUtils.extractUserId(jwt))) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }

        User updated = userService.updateUser(id, request.email(), request.fullName());
        return ResponseEntity.ok(UserResponse.from(updated));
    }

    @PreAuthorize("hasAuthority('ADMIN')")
    @PostMapping("/{id}/promote")
    public ResponseEntity<UserResponse> promoteUserToAdmin(@PathVariable UUID id) {
        User promoted = userService.promoteUserToAdmin(id);
        return ResponseEntity.ok(UserResponse.from(promoted));
    }
}
```

- [ ] **Step 4: Run tests to verify they pass**

Run: `./mvnw test -pl . -Dtest=UserControllerTest -q`
Expected: BUILD SUCCESS

- [ ] **Step 5: Commit**

```bash
git add src/main/java/com/financialhub/financialhubapi/infrastructure/web/UserController.java \
        src/test/java/com/financialhub/financialhubapi/web/UserControllerTest.java
git commit -m "feat: enforce ownership in UserController, MEMBER gets 404 for other users"
```

---

## Task 12: Final full test suite verification

- [ ] **Step 1: Run all tests**

Run: `./mvnw test -q`
Expected: BUILD SUCCESS, zero test failures

- [ ] **Step 2: Commit if any cleanup was needed**

If all tests pass with no changes, no commit needed. If minor fixes were applied, commit them:

```bash
git add -p
git commit -m "fix: address remaining test failures after JWT extraction refactor"
```

---

## Verification Checklist

End-to-end scenarios to verify manually with a running app (or integration tests):

1. `POST /api/v1/assets` — MEMBER, no `userId` in body → 201, asset belongs to JWT user
2. `POST /api/v1/assets` — MEMBER, `userId` = another user's id in body → 201, asset still belongs to JWT user
3. `POST /api/v1/assets` — ADMIN, `userId` = target user in body → 201, asset belongs to target user
4. `GET /api/v1/assets` — MEMBER, no `userId` param → 200, returns own assets
5. `GET /api/v1/assets?userId=xxx` — ADMIN → 200, returns target user's assets
6. `PUT /api/v1/assets/{id}` — MEMBER, asset belongs to them → 200
7. `PUT /api/v1/assets/{id}` — MEMBER, asset belongs to other user → 404
8. `GET /api/v1/summary/{userId}` — MEMBER, own id → 200
9. `GET /api/v1/summary/{otherId}` — MEMBER → 404
10. `GET /api/v1/summary/{anyId}` — ADMIN → 200
11. `GET /api/v1/users/{id}` — MEMBER, own id → 200
12. `GET /api/v1/users/{otherId}` — MEMBER → 404
13. `PUT /api/v1/users/{id}` — MEMBER, own id → 200
14. `PUT /api/v1/users/{otherId}` — MEMBER → 404
15. `DELETE /api/v1/summary-subscriptions/{id}` — MEMBER, own subscription → 204
16. `DELETE /api/v1/summary-subscriptions/{id}` — MEMBER, other user's subscription → 404
