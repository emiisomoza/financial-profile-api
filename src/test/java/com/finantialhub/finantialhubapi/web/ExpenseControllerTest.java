package com.finantialhub.finantialhubapi.web;

import com.finantialhub.finantialhubapi.infrastructure.web.ExpenseController;
import tools.jackson.databind.ObjectMapper;
import com.finantialhub.finantialhubapi.application.usecases.ExpenseService;
import com.finantialhub.finantialhubapi.domain.model.Expense;
import com.finantialhub.finantialhubapi.domain.model.ExpenseCategory;
import com.finantialhub.finantialhubapi.domain.model.ExpenseFrequency;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.security.web.method.annotation.AuthenticationPrincipalArgumentResolver;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import com.finantialhub.finantialhubapi.domain.exceptions.ExpenseNotFoundException;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = ExpenseController.class)
class ExpenseControllerTest {

    @TestConfiguration
    static class SecurityTestConfig implements WebMvcConfigurer {
        @Override
        public void addArgumentResolvers(List<HandlerMethodArgumentResolver> resolvers) {
            resolvers.add(new AuthenticationPrincipalArgumentResolver());
        }
    }

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;

    @MockitoBean ExpenseService expenseService;

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    private static RequestPostProcessor memberJwt(UUID userId) {
        return jwtContext(userId, "MEMBER");
    }

    private static RequestPostProcessor adminJwt(UUID adminId) {
        return jwtContext(adminId, "ADMIN");
    }

    private static RequestPostProcessor jwtContext(UUID userId, String role) {
        return request -> {
            Jwt jwt = Jwt.withTokenValue("test-token")
                    .header("alg", "HS256")
                    .subject(userId.toString())
                    .claim("role", role)
                    .build();
            JwtAuthenticationToken auth = new JwtAuthenticationToken(jwt,
                    List.of(new SimpleGrantedAuthority(role)));
            SecurityContext context = SecurityContextHolder.createEmptyContext();
            context.setAuthentication(auth);
            SecurityContextHolder.setContext(context);
            return request;
        };
    }

    private Expense sampleExpense(UUID userId) {
        return new Expense(
                UUID.randomUUID(), userId,
                ExpenseCategory.RENT, "Rent payment",
                ExpenseFrequency.MONTHLY,
                new BigDecimal("2400.00"), "AUD",
                LocalDate.parse("2026-01-01"), null,
                Instant.now()
        );
    }

    // ── POST /api/v1/expenses ─────────────────────────────────────────────────

    @Test
    void createExpense_memberExtractsUserIdFromJwt() throws Exception {
        UUID userId = UUID.randomUUID();
        Expense expense = sampleExpense(userId);
        when(expenseService.createExpense(any())).thenReturn(expense);

        mockMvc.perform(post("/api/v1/expenses")
                        .with(memberJwt(userId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {"category":"RENT","description":"Rent payment","frequency":"MONTHLY",
                             "amount":2400.00,"currency":"AUD","startsAt":"2026-01-01"}
                            """))
                .andExpect(status().isCreated())
                .andExpect(header().exists("Location"))
                .andExpect(jsonPath("$.userId").value(userId.toString()))
                .andExpect(jsonPath("$.category").value("RENT"))
                .andExpect(jsonPath("$.amount").value(2400.00));
    }

    @Test
    void createExpense_adminCanSpecifyAnotherUserId() throws Exception {
        UUID adminId = UUID.randomUUID();
        UUID targetUserId = UUID.randomUUID();
        Expense expense = sampleExpense(targetUserId);
        when(expenseService.createExpense(any())).thenReturn(expense);

        mockMvc.perform(post("/api/v1/expenses")
                        .with(adminJwt(adminId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {"userId":"%s","category":"RENT","description":"Rent payment","frequency":"MONTHLY",
                             "amount":2400.00,"currency":"AUD","startsAt":"2026-01-01"}
                            """.formatted(targetUserId)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.userId").value(targetUserId.toString()));
    }

    // ── GET /api/v1/expenses ──────────────────────────────────────────────────

    @Test
    void listExpenses_memberGetsOwnExpenses() throws Exception {
        UUID userId = UUID.randomUUID();
        Expense expense = sampleExpense(userId);
        when(expenseService.getExpensesForUser(userId)).thenReturn(List.of(expense));

        mockMvc.perform(get("/api/v1/expenses")
                        .with(memberJwt(userId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].userId").value(userId.toString()))
                .andExpect(jsonPath("$[0].category").value("RENT"));
    }

    @Test
    void listExpenses_adminCanQueryOtherUser() throws Exception {
        UUID adminId = UUID.randomUUID();
        UUID targetUserId = UUID.randomUUID();
        Expense expense = sampleExpense(targetUserId);
        when(expenseService.getExpensesForUser(targetUserId)).thenReturn(List.of(expense));

        mockMvc.perform(get("/api/v1/expenses")
                        .param("userId", targetUserId.toString())
                        .with(adminJwt(adminId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].userId").value(targetUserId.toString()));
    }

    // ── PUT /api/v1/expenses/{id} ─────────────────────────────────────────────

    @Test
    void updateExpense_memberCanUpdateOwnExpense() throws Exception {
        UUID userId = UUID.randomUUID();
        UUID expenseId = UUID.randomUUID();
        Expense updated = new Expense(
                expenseId, userId,
                ExpenseCategory.GROCERIES, "Weekly groceries",
                ExpenseFrequency.WEEKLY,
                new BigDecimal("300.00"), "AUD",
                LocalDate.parse("2026-02-01"), null,
                Instant.now()
        );
        when(expenseService.getExpenseById(expenseId)).thenReturn(sampleExpense(userId));
        when(expenseService.updateExpense(eq(expenseId), any())).thenReturn(updated);

        mockMvc.perform(put("/api/v1/expenses/" + expenseId)
                        .with(memberJwt(userId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {"category":"GROCERIES","description":"Weekly groceries","frequency":"WEEKLY",
                             "amount":300.00,"currency":"AUD","startsAt":"2026-02-01"}
                            """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.category").value("GROCERIES"))
                .andExpect(jsonPath("$.frequency").value("WEEKLY"))
                .andExpect(jsonPath("$.amount").value(300.00));
    }

    @Test
    void updateExpense_memberCannotUpdateOtherUsersExpense() throws Exception {
        UUID userId = UUID.randomUUID();
        UUID otherUserId = UUID.randomUUID();
        UUID expenseId = UUID.randomUUID();
        when(expenseService.getExpenseById(expenseId)).thenReturn(sampleExpense(otherUserId));

        mockMvc.perform(put("/api/v1/expenses/" + expenseId)
                        .with(memberJwt(userId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {"category":"RENT","description":"Rent payment","frequency":"MONTHLY",
                             "amount":2400.00,"currency":"AUD","startsAt":"2026-01-01"}
                            """))
                .andExpect(status().isNotFound());
    }

    @Test
    void updateExpense_adminCanUpdateAnyExpense() throws Exception {
        UUID adminId = UUID.randomUUID();
        UUID targetUserId = UUID.randomUUID();
        UUID expenseId = UUID.randomUUID();
        Expense updated = new Expense(
                expenseId, targetUserId,
                ExpenseCategory.UTILITIES, "Electricity bill",
                ExpenseFrequency.MONTHLY,
                new BigDecimal("150.00"), "AUD",
                LocalDate.parse("2026-03-01"), null,
                Instant.now()
        );
        when(expenseService.getExpenseById(expenseId)).thenReturn(sampleExpense(targetUserId));
        when(expenseService.updateExpense(eq(expenseId), any())).thenReturn(updated);

        mockMvc.perform(put("/api/v1/expenses/" + expenseId)
                        .with(adminJwt(adminId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {"category":"UTILITIES","description":"Electricity bill","frequency":"MONTHLY",
                             "amount":150.00,"currency":"AUD","startsAt":"2026-03-01"}
                            """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.category").value("UTILITIES"))
                .andExpect(jsonPath("$.description").value("Electricity bill"));
    }

    @Test
    void updateExpense_returns404WhenExpenseNotFound() throws Exception {
        UUID userId = UUID.randomUUID();
        UUID expenseId = UUID.randomUUID();
        when(expenseService.getExpenseById(expenseId)).thenThrow(new ExpenseNotFoundException(expenseId));

        mockMvc.perform(put("/api/v1/expenses/" + expenseId)
                        .with(memberJwt(userId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {"category":"RENT","description":"Rent payment","frequency":"MONTHLY",
                             "amount":2400.00,"currency":"AUD","startsAt":"2026-01-01"}
                            """))
                .andExpect(status().isNotFound());
    }

    // ── DELETE /api/v1/expenses/{id} ─────────────────────────────────────────

    @Test
    void deleteExpense_memberCanDeleteOwnExpense() throws Exception {
        UUID userId = UUID.randomUUID();
        UUID expenseId = UUID.randomUUID();
        when(expenseService.getExpenseById(expenseId)).thenReturn(sampleExpense(userId));
        doNothing().when(expenseService).deleteExpense(expenseId);

        mockMvc.perform(delete("/api/v1/expenses/" + expenseId)
                        .with(memberJwt(userId)))
                .andExpect(status().isNoContent());

        verify(expenseService).deleteExpense(expenseId);
    }

    @Test
    void deleteExpense_memberCannotDeleteOtherUsersExpense() throws Exception {
        UUID userId = UUID.randomUUID();
        UUID otherUserId = UUID.randomUUID();
        UUID expenseId = UUID.randomUUID();
        when(expenseService.getExpenseById(expenseId)).thenReturn(sampleExpense(otherUserId));

        mockMvc.perform(delete("/api/v1/expenses/" + expenseId)
                        .with(memberJwt(userId)))
                .andExpect(status().isNotFound());
    }

    @Test
    void deleteExpense_adminCanDeleteAnyExpense() throws Exception {
        UUID adminId = UUID.randomUUID();
        UUID targetUserId = UUID.randomUUID();
        UUID expenseId = UUID.randomUUID();
        when(expenseService.getExpenseById(expenseId)).thenReturn(sampleExpense(targetUserId));
        doNothing().when(expenseService).deleteExpense(expenseId);

        mockMvc.perform(delete("/api/v1/expenses/" + expenseId)
                        .with(adminJwt(adminId)))
                .andExpect(status().isNoContent());

        verify(expenseService).deleteExpense(expenseId);
    }

    @Test
    void deleteExpense_returns404WhenNotFound() throws Exception {
        UUID userId = UUID.randomUUID();
        UUID expenseId = UUID.randomUUID();
        when(expenseService.getExpenseById(expenseId)).thenThrow(new ExpenseNotFoundException(expenseId));

        mockMvc.perform(delete("/api/v1/expenses/" + expenseId)
                        .with(memberJwt(userId)))
                .andExpect(status().isNotFound());
    }
}
