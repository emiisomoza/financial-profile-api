package com.finantialhub.finantialhubapi.web;

import tools.jackson.databind.ObjectMapper;
import com.finantialhub.finantialhubapi.application.usecases.SummarySubscriptionService;
import com.finantialhub.finantialhubapi.domain.exceptions.SubscriptionNotFoundException;
import com.finantialhub.finantialhubapi.domain.exceptions.UserNotFoundException;
import com.finantialhub.finantialhubapi.domain.model.SummarySubscription;
import com.finantialhub.finantialhubapi.infrastructure.web.SummarySubscriptionController;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
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

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static com.finantialhub.finantialhubapi.domain.model.SummarySubscription.Frequency.MONTHLY;
import static com.finantialhub.finantialhubapi.domain.model.SummarySubscription.Frequency.WEEKLY;
import static com.finantialhub.finantialhubapi.domain.model.SummarySubscription.Status.ACTIVE;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = SummarySubscriptionController.class)
class SummarySubscriptionControllerTest {

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

    // ── POST /api/v1/summary-subscriptions ───────────────────────────────────

    @Test
    void create_memberExtractsUserIdFromJwt() throws Exception {
        when(subscriptionService.create(userId, WEEKLY, "AUD")).thenReturn(activeSubscription);

        mockMvc.perform(post("/api/v1/summary-subscriptions")
                        .with(memberJwt(userId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "frequency": "WEEKLY", "currency": "AUD" }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(subscriptionId.toString()))
                .andExpect(jsonPath("$.userId").value(userId.toString()))
                .andExpect(jsonPath("$.frequency").value("WEEKLY"))
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andExpect(jsonPath("$.currency").value("AUD"));
    }

    @Test
    void create_adminCanSpecifyAnotherUserId() throws Exception {
        UUID adminId = UUID.randomUUID();
        when(subscriptionService.create(userId, WEEKLY, "AUD")).thenReturn(activeSubscription);

        mockMvc.perform(post("/api/v1/summary-subscriptions")
                        .with(adminJwt(adminId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "userId": "%s", "frequency": "WEEKLY", "currency": "AUD" }
                                """.formatted(userId)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.userId").value(userId.toString()));
    }

    @Test
    void create_whenUserNotFound_returns404() throws Exception {
        when(subscriptionService.create(any(), any(), any()))
                .thenThrow(new UserNotFoundException("User not found with id: " + userId));

        mockMvc.perform(post("/api/v1/summary-subscriptions")
                        .with(memberJwt(userId))
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
                        .with(memberJwt(userId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "frequency": "WEEKLY", "currency": "australian_dollar" }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.fieldErrors.currency").exists());
    }

    @Test
    void create_whenInvalidFrequency_returns400() throws Exception {
        mockMvc.perform(post("/api/v1/summary-subscriptions")
                        .with(memberJwt(userId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "frequency": "DAILY", "currency": "AUD" }
                                """))
                .andExpect(status().isBadRequest());
    }

    // ── GET /api/v1/summary-subscriptions/user/{userId} ──────────────────────

    @Test
    void getActiveForUser_memberGetsOwnSubscription() throws Exception {
        when(subscriptionService.getActiveForUser(userId)).thenReturn(activeSubscription);

        mockMvc.perform(get("/api/v1/summary-subscriptions/user/{userId}", userId)
                        .with(memberJwt(userId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(userId.toString()))
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andExpect(jsonPath("$.frequency").value("WEEKLY"));
    }

    @Test
    void getActiveForUser_memberCannotAccessOtherUser() throws Exception {
        UUID otherUserId = UUID.randomUUID();

        mockMvc.perform(get("/api/v1/summary-subscriptions/user/{userId}", otherUserId)
                        .with(memberJwt(userId)))
                .andExpect(status().isNotFound());
    }

    @Test
    void getActiveForUser_whenNoneActive_returns404() throws Exception {
        when(subscriptionService.getActiveForUser(userId))
                .thenThrow(new SubscriptionNotFoundException("No active subscription for user: " + userId));

        mockMvc.perform(get("/api/v1/summary-subscriptions/user/{userId}", userId)
                        .with(memberJwt(userId)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("SUBSCRIPTION_NOT_FOUND"));
    }

    // ── PUT /api/v1/summary-subscriptions/{id} ────────────────────────────────

    @Test
    void updateFrequency_memberCanUpdateOwnSubscription() throws Exception {
        SummarySubscription updated = new SummarySubscription(
                subscriptionId, userId, MONTHLY, "AUD",
                ACTIVE, Instant.now().plusSeconds(3600), Instant.now()
        );
        when(subscriptionService.getSubscriptionById(subscriptionId)).thenReturn(activeSubscription);
        when(subscriptionService.updateFrequency(subscriptionId, MONTHLY)).thenReturn(updated);

        mockMvc.perform(put("/api/v1/summary-subscriptions/{id}", subscriptionId)
                        .with(memberJwt(userId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "frequency": "MONTHLY" }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.frequency").value("MONTHLY"))
                .andExpect(jsonPath("$.status").value("ACTIVE"));
    }

    @Test
    void updateFrequency_memberCannotUpdateOtherUsersSubscription() throws Exception {
        UUID otherUserId = UUID.randomUUID();
        SummarySubscription otherSubscription = new SummarySubscription(
                subscriptionId, otherUserId, WEEKLY, "AUD",
                ACTIVE, Instant.now().plusSeconds(3600), Instant.now()
        );
        when(subscriptionService.getSubscriptionById(subscriptionId)).thenReturn(otherSubscription);

        mockMvc.perform(put("/api/v1/summary-subscriptions/{id}", subscriptionId)
                        .with(memberJwt(userId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "frequency": "MONTHLY" }
                                """))
                .andExpect(status().isNotFound());
    }

    @Test
    void updateFrequency_whenNotFound_returns404() throws Exception {
        when(subscriptionService.getSubscriptionById(subscriptionId))
                .thenThrow(new SubscriptionNotFoundException("Subscription not found: " + subscriptionId));

        mockMvc.perform(put("/api/v1/summary-subscriptions/{id}", subscriptionId)
                        .with(memberJwt(userId))
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
                        .with(memberJwt(userId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }

    // ── DELETE /api/v1/summary-subscriptions/{id} ────────────────────────────

    @Test
    void cancel_memberCanCancelOwnSubscription() throws Exception {
        when(subscriptionService.getSubscriptionById(subscriptionId)).thenReturn(activeSubscription);
        doNothing().when(subscriptionService).cancel(subscriptionId);

        mockMvc.perform(delete("/api/v1/summary-subscriptions/{id}", subscriptionId)
                        .with(memberJwt(userId)))
                .andExpect(status().isNoContent());

        verify(subscriptionService).cancel(subscriptionId);
    }

    @Test
    void cancel_whenNotFound_returns404() throws Exception {
        when(subscriptionService.getSubscriptionById(subscriptionId))
                .thenThrow(new SubscriptionNotFoundException("Subscription not found: " + subscriptionId));

        mockMvc.perform(delete("/api/v1/summary-subscriptions/{id}", subscriptionId)
                        .with(memberJwt(userId)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("SUBSCRIPTION_NOT_FOUND"));
    }
}
