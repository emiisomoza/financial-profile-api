package com.financialhub.financialhubapi.web;

import com.financialhub.financialhubapi.infrastructure.web.IncomeController;
import tools.jackson.databind.ObjectMapper;
import com.financialhub.financialhubapi.application.usecases.IncomeService;
import com.financialhub.financialhubapi.domain.model.Income;
import com.financialhub.financialhubapi.domain.model.IncomeFrequency;
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

import com.financialhub.financialhubapi.domain.exceptions.IncomeNotFoundException;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = IncomeController.class)
class IncomeControllerTest {

    @TestConfiguration
    static class SecurityTestConfig implements WebMvcConfigurer {
        @Override
        public void addArgumentResolvers(List<HandlerMethodArgumentResolver> resolvers) {
            resolvers.add(new AuthenticationPrincipalArgumentResolver());
        }
    }

    @Autowired
    MockMvc mockMvc;

    @Autowired
    ObjectMapper objectMapper;

    @MockitoBean
    IncomeService incomeService;

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

    private Income sampleIncome(UUID userId) {
        return new Income(
                UUID.randomUUID(), userId,
                "Salary", IncomeFrequency.MONTHLY,
                new BigDecimal("9000.00"), "AUD",
                LocalDate.parse("2026-01-01"), null,
                Instant.now()
        );
    }

    // ── POST /api/v1/incomes ──────────────────────────────────────────────────

    @Test
    void createIncome_memberExtractsUserIdFromJwt() throws Exception {
        UUID userId = UUID.randomUUID();
        Income income = sampleIncome(userId);
        when(incomeService.createIncome(any())).thenReturn(income);

        mockMvc.perform(post("/api/v1/incomes")
                        .with(memberJwt(userId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {"source":"Salary","frequency":"MONTHLY",
                             "amount":9000.00,"currency":"AUD","startsAt":"2026-01-01"}
                            """))
                .andExpect(status().isCreated())
                .andExpect(header().exists("Location"))
                .andExpect(jsonPath("$.userId").value(userId.toString()))
                .andExpect(jsonPath("$.source").value("Salary"))
                .andExpect(jsonPath("$.frequency").value("MONTHLY"))
                .andExpect(jsonPath("$.amount").value(9000.00))
                .andExpect(jsonPath("$.currency").value("AUD"));
    }

    @Test
    void createIncome_adminCanSpecifyAnotherUserId() throws Exception {
        UUID adminId = UUID.randomUUID();
        UUID targetUserId = UUID.randomUUID();
        Income income = sampleIncome(targetUserId);
        when(incomeService.createIncome(any())).thenReturn(income);

        mockMvc.perform(post("/api/v1/incomes")
                        .with(adminJwt(adminId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {"userId":"%s","source":"Salary","frequency":"MONTHLY",
                             "amount":9000.00,"currency":"AUD","startsAt":"2026-01-01"}
                            """.formatted(targetUserId)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.userId").value(targetUserId.toString()));
    }

    // ── GET /api/v1/incomes ───────────────────────────────────────────────────

    @Test
    void listIncomes_memberGetsOwnIncomes() throws Exception {
        UUID userId = UUID.randomUUID();
        Income income = sampleIncome(userId);
        when(incomeService.getIncomesForUser(userId)).thenReturn(List.of(income));

        mockMvc.perform(get("/api/v1/incomes")
                        .with(memberJwt(userId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].userId").value(userId.toString()))
                .andExpect(jsonPath("$[0].frequency").value("MONTHLY"));
    }

    @Test
    void listIncomes_adminCanQueryOtherUser() throws Exception {
        UUID adminId = UUID.randomUUID();
        UUID targetUserId = UUID.randomUUID();
        Income income = sampleIncome(targetUserId);
        when(incomeService.getIncomesForUser(targetUserId)).thenReturn(List.of(income));

        mockMvc.perform(get("/api/v1/incomes")
                        .param("userId", targetUserId.toString())
                        .with(adminJwt(adminId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].userId").value(targetUserId.toString()));
    }

    // ── PUT /api/v1/incomes/{id} ──────────────────────────────────────────────

    @Test
    void updateIncome_memberCanUpdateOwnIncome() throws Exception {
        UUID userId = UUID.randomUUID();
        UUID incomeId = UUID.randomUUID();
        Income updated = new Income(
                incomeId, userId,
                "Freelance", IncomeFrequency.FORTNIGHTLY,
                new BigDecimal("10000.00"), "USD",
                LocalDate.parse("2026-02-01"), null,
                Instant.now()
        );
        when(incomeService.getIncomeById(incomeId)).thenReturn(sampleIncome(userId));
        when(incomeService.updateIncome(eq(incomeId), any())).thenReturn(updated);

        mockMvc.perform(put("/api/v1/incomes/" + incomeId)
                        .with(memberJwt(userId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {"source":"Freelance","frequency":"FORTNIGHTLY",
                             "amount":10000.00,"currency":"USD","startsAt":"2026-02-01"}
                            """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.source").value("Freelance"))
                .andExpect(jsonPath("$.frequency").value("FORTNIGHTLY"))
                .andExpect(jsonPath("$.amount").value(10000.00));
    }

    @Test
    void updateIncome_memberCannotUpdateOtherUsersIncome() throws Exception {
        UUID userId = UUID.randomUUID();
        UUID otherUserId = UUID.randomUUID();
        UUID incomeId = UUID.randomUUID();
        when(incomeService.getIncomeById(incomeId)).thenReturn(sampleIncome(otherUserId));

        mockMvc.perform(put("/api/v1/incomes/" + incomeId)
                        .with(memberJwt(userId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {"source":"Freelance","frequency":"MONTHLY",
                             "amount":5000.00,"currency":"AUD","startsAt":"2026-01-01"}
                            """))
                .andExpect(status().isNotFound());
    }

    @Test
    void updateIncome_adminCanUpdateAnyIncome() throws Exception {
        UUID adminId = UUID.randomUUID();
        UUID targetUserId = UUID.randomUUID();
        UUID incomeId = UUID.randomUUID();
        Income updated = new Income(
                incomeId, targetUserId,
                "Consulting", IncomeFrequency.MONTHLY,
                new BigDecimal("12000.00"), "USD",
                LocalDate.parse("2026-03-01"), null,
                Instant.now()
        );
        when(incomeService.getIncomeById(incomeId)).thenReturn(sampleIncome(targetUserId));
        when(incomeService.updateIncome(eq(incomeId), any())).thenReturn(updated);

        mockMvc.perform(put("/api/v1/incomes/" + incomeId)
                        .with(adminJwt(adminId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {"source":"Consulting","frequency":"MONTHLY",
                             "amount":12000.00,"currency":"USD","startsAt":"2026-03-01"}
                            """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.source").value("Consulting"));
    }

    @Test
    void updateIncome_returns404WhenIncomeNotFound() throws Exception {
        UUID userId = UUID.randomUUID();
        UUID incomeId = UUID.randomUUID();
        when(incomeService.getIncomeById(incomeId)).thenThrow(new IncomeNotFoundException(incomeId));

        mockMvc.perform(put("/api/v1/incomes/" + incomeId)
                        .with(memberJwt(userId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {"source":"Freelance","frequency":"MONTHLY",
                             "amount":5000.00,"currency":"AUD","startsAt":"2026-01-01"}
                            """))
                .andExpect(status().isNotFound());
    }

    // ── DELETE /api/v1/incomes/{id} ───────────────────────────────────────────

    @Test
    void deleteIncome_memberCanDeleteOwnIncome() throws Exception {
        UUID userId = UUID.randomUUID();
        UUID incomeId = UUID.randomUUID();
        when(incomeService.getIncomeById(incomeId)).thenReturn(sampleIncome(userId));
        doNothing().when(incomeService).deleteIncome(incomeId);

        mockMvc.perform(delete("/api/v1/incomes/" + incomeId)
                        .with(memberJwt(userId)))
                .andExpect(status().isNoContent());

        verify(incomeService).deleteIncome(incomeId);
    }

    @Test
    void deleteIncome_memberCannotDeleteOtherUsersIncome() throws Exception {
        UUID userId = UUID.randomUUID();
        UUID otherUserId = UUID.randomUUID();
        UUID incomeId = UUID.randomUUID();
        when(incomeService.getIncomeById(incomeId)).thenReturn(sampleIncome(otherUserId));

        mockMvc.perform(delete("/api/v1/incomes/" + incomeId)
                        .with(memberJwt(userId)))
                .andExpect(status().isNotFound());
    }

    @Test
    void deleteIncome_adminCanDeleteAnyIncome() throws Exception {
        UUID adminId = UUID.randomUUID();
        UUID targetUserId = UUID.randomUUID();
        UUID incomeId = UUID.randomUUID();
        when(incomeService.getIncomeById(incomeId)).thenReturn(sampleIncome(targetUserId));
        doNothing().when(incomeService).deleteIncome(incomeId);

        mockMvc.perform(delete("/api/v1/incomes/" + incomeId)
                        .with(adminJwt(adminId)))
                .andExpect(status().isNoContent());

        verify(incomeService).deleteIncome(incomeId);
    }

    @Test
    void deleteIncome_returns404WhenNotFound() throws Exception {
        UUID userId = UUID.randomUUID();
        UUID incomeId = UUID.randomUUID();
        when(incomeService.getIncomeById(incomeId)).thenThrow(new IncomeNotFoundException(incomeId));

        mockMvc.perform(delete("/api/v1/incomes/" + incomeId)
                        .with(memberJwt(userId)))
                .andExpect(status().isNotFound());
    }
}
