package com.finantialhub.finantialhubapi.web;

import com.finantialhub.finantialhubapi.application.usecases.SummaryService;
import com.finantialhub.finantialhubapi.infrastructure.web.SummaryController;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.UUID;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = SummaryController.class)
class SummaryControllerTest {

    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    SummaryService summaryService;

    @Test
    void getSummary_returns200WithSummaryBody() throws Exception {
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

        mockMvc.perform(get("/api/v1/summary/{userId}", userId))
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

        mockMvc.perform(get("/api/v1/summary/{userId}", userId).param("currency", "USD"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.currency").value("USD"));
    }
}
