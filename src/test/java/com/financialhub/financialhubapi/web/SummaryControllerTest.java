package com.financialhub.financialhubapi.web;

import com.financialhub.financialhubapi.application.usecases.SummaryService;
import com.financialhub.financialhubapi.infrastructure.web.SummaryController;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
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
import java.util.List;
import java.util.UUID;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = SummaryController.class)
class SummaryControllerTest {

    @TestConfiguration
    static class SecurityTestConfig implements WebMvcConfigurer {
        @Override
        public void addArgumentResolvers(List<HandlerMethodArgumentResolver> resolvers) {
            resolvers.add(new AuthenticationPrincipalArgumentResolver());
        }
    }

    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    SummaryService summaryService;

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

    @Test
    void getSummary_memberGetsOwnSummary() throws Exception {
        UUID userId = UUID.randomUUID();
        SummaryService.Summary summary = new SummaryService.Summary(
                userId, "AUD",
                new BigDecimal("150000"),
                new BigDecimal("5000"),
                new BigDecimal("3000"),
                new BigDecimal("2000"),
                new BigDecimal("0.40"),
                0
        );
        when(summaryService.getSummary(userId, "AUD")).thenReturn(summary);

        mockMvc.perform(get("/api/v1/summary/{userId}", userId)
                        .with(memberJwt(userId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.currency").value("AUD"))
                .andExpect(jsonPath("$.totalAssetsValue").value(150000))
                .andExpect(jsonPath("$.monthlyIncome").value(5000))
                .andExpect(jsonPath("$.monthlySavings").value(2000))
                .andExpect(jsonPath("$.unpricedAssetsCount").value(0));
    }

    @Test
    void getSummary_withExplicitCurrency_passesItToService() throws Exception {
        UUID userId = UUID.randomUUID();
        SummaryService.Summary summary = new SummaryService.Summary(
                userId, "USD",
                BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, 0
        );
        when(summaryService.getSummary(userId, "USD")).thenReturn(summary);

        mockMvc.perform(get("/api/v1/summary/{userId}", userId)
                        .param("currency", "USD")
                        .with(memberJwt(userId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.currency").value("USD"));
    }

    @Test
    void getSummary_memberCannotAccessOtherUserSummary() throws Exception {
        UUID userId = UUID.randomUUID();
        UUID otherUserId = UUID.randomUUID();

        mockMvc.perform(get("/api/v1/summary/{userId}", otherUserId)
                        .with(memberJwt(userId)))
                .andExpect(status().isNotFound());
    }

    @Test
    void getSummary_adminCanAccessAnyUserSummary() throws Exception {
        UUID adminId = UUID.randomUUID();
        UUID targetUserId = UUID.randomUUID();
        SummaryService.Summary summary = new SummaryService.Summary(
                targetUserId, "AUD",
                BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, 0
        );
        when(summaryService.getSummary(targetUserId, "AUD")).thenReturn(summary);

        mockMvc.perform(get("/api/v1/summary/{userId}", targetUserId)
                        .with(adminJwt(adminId)))
                .andExpect(status().isOk());
    }
}
