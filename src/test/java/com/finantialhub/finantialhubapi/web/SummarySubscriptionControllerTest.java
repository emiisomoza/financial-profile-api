package com.finantialhub.finantialhubapi.web;

import tools.jackson.databind.ObjectMapper;
import com.finantialhub.finantialhubapi.application.usecases.SummarySubscriptionService;
import com.finantialhub.finantialhubapi.domain.exceptions.SubscriptionNotFoundException;
import com.finantialhub.finantialhubapi.domain.exceptions.UserNotFoundException;
import com.finantialhub.finantialhubapi.domain.model.SummarySubscription;
import com.finantialhub.finantialhubapi.infrastructure.web.SummarySubscriptionController;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.UUID;

import static com.finantialhub.finantialhubapi.domain.model.SummarySubscription.Frequency.MONTHLY;
import static com.finantialhub.finantialhubapi.domain.model.SummarySubscription.Frequency.WEEKLY;
import static com.finantialhub.finantialhubapi.domain.model.SummarySubscription.Status.ACTIVE;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = SummarySubscriptionController.class)
class SummarySubscriptionControllerTest {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    ObjectMapper objectMapper;

    @MockitoBean
    SummarySubscriptionService subscriptionService;

    private UUID userId;
    private UUID subscriptionId;
    private SummarySubscription activeSubscription;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        subscriptionId = UUID.randomUUID();
        activeSubscription = new SummarySubscription(
                subscriptionId, userId, WEEKLY, "AUD",
                ACTIVE, Instant.now().plusSeconds(3600), Instant.now()
        );
    }

    // ── POST /api/v1/summary-subscriptions ───────────────────────────────────

    @Test
    void create_returns201WithSubscriptionBody() throws Exception {
        when(subscriptionService.create(userId, WEEKLY, "AUD")).thenReturn(activeSubscription);

        mockMvc.perform(post("/api/v1/summary-subscriptions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "userId": "%s", "frequency": "WEEKLY", "currency": "AUD" }
                                """.formatted(userId)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(subscriptionId.toString()))
                .andExpect(jsonPath("$.userId").value(userId.toString()))
                .andExpect(jsonPath("$.frequency").value("WEEKLY"))
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andExpect(jsonPath("$.currency").value("AUD"));
    }

    @Test
    void create_whenUserNotFound_returns404() throws Exception {
        when(subscriptionService.create(any(), any(), any()))
                .thenThrow(new UserNotFoundException("User not found with id: " + userId));

        mockMvc.perform(post("/api/v1/summary-subscriptions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "userId": "%s", "frequency": "WEEKLY", "currency": "AUD" }
                                """.formatted(userId)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("USER_NOT_FOUND"));
    }

    @Test
    void create_whenMissingUserId_returns400() throws Exception {
        mockMvc.perform(post("/api/v1/summary-subscriptions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "frequency": "WEEKLY", "currency": "AUD" }
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void create_whenInvalidCurrencyFormat_returns400() throws Exception {
        mockMvc.perform(post("/api/v1/summary-subscriptions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "userId": "%s", "frequency": "WEEKLY", "currency": "australian_dollar" }
                                """.formatted(userId)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.fieldErrors.currency").exists());
    }

    @Test
    void create_whenInvalidFrequency_returns400() throws Exception {
        mockMvc.perform(post("/api/v1/summary-subscriptions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "userId": "%s", "frequency": "DAILY", "currency": "AUD" }
                                """.formatted(userId)))
                .andExpect(status().isBadRequest());
    }

    // ── GET /api/v1/summary-subscriptions/user/{userId} ──────────────────────

    @Test
    void getActiveForUser_returns200WithSubscription() throws Exception {
        when(subscriptionService.getActiveForUser(userId)).thenReturn(activeSubscription);

        mockMvc.perform(get("/api/v1/summary-subscriptions/user/{userId}", userId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(userId.toString()))
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andExpect(jsonPath("$.frequency").value("WEEKLY"));
    }

    @Test
    void getActiveForUser_whenNoneActive_returns404() throws Exception {
        when(subscriptionService.getActiveForUser(userId))
                .thenThrow(new SubscriptionNotFoundException("No active subscription for user: " + userId));

        mockMvc.perform(get("/api/v1/summary-subscriptions/user/{userId}", userId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("SUBSCRIPTION_NOT_FOUND"));
    }

    // ── PUT /api/v1/summary-subscriptions/{id} ────────────────────────────────

    @Test
    void updateFrequency_returns200WithUpdatedSubscription() throws Exception {
        SummarySubscription updated = new SummarySubscription(
                subscriptionId, userId, MONTHLY, "AUD",
                ACTIVE, Instant.now().plusSeconds(3600), Instant.now()
        );
        when(subscriptionService.updateFrequency(subscriptionId, MONTHLY)).thenReturn(updated);

        mockMvc.perform(put("/api/v1/summary-subscriptions/{id}", subscriptionId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "frequency": "MONTHLY" }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.frequency").value("MONTHLY"))
                .andExpect(jsonPath("$.status").value("ACTIVE"));
    }

    @Test
    void updateFrequency_whenNotFound_returns404() throws Exception {
        when(subscriptionService.updateFrequency(eq(subscriptionId), any()))
                .thenThrow(new SubscriptionNotFoundException("Subscription not found: " + subscriptionId));

        mockMvc.perform(put("/api/v1/summary-subscriptions/{id}", subscriptionId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "frequency": "MONTHLY" }
                                """))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("SUBSCRIPTION_NOT_FOUND"));
    }

    @Test
    void updateFrequency_whenMissingFrequency_returns400() throws Exception {
        mockMvc.perform(put("/api/v1/summary-subscriptions/{id}", subscriptionId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }

    // ── DELETE /api/v1/summary-subscriptions/{id} ────────────────────────────

    @Test
    void cancel_returns204() throws Exception {
        doNothing().when(subscriptionService).cancel(subscriptionId);

        mockMvc.perform(delete("/api/v1/summary-subscriptions/{id}", subscriptionId))
                .andExpect(status().isNoContent());

        verify(subscriptionService).cancel(subscriptionId);
    }

    @Test
    void cancel_whenNotFound_returns404() throws Exception {
        doThrow(new SubscriptionNotFoundException("Subscription not found: " + subscriptionId))
                .when(subscriptionService).cancel(subscriptionId);

        mockMvc.perform(delete("/api/v1/summary-subscriptions/{id}", subscriptionId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("SUBSCRIPTION_NOT_FOUND"));
    }
}
