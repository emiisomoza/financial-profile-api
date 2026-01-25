package com.finantialhub.finantialhubapi.web;

import com.finantialhub.finantialhubapi.infrastructure.web.IncomeController;
import tools.jackson.databind.ObjectMapper;
import com.finantialhub.finantialhubapi.application.usecases.IncomeService;
import com.finantialhub.finantialhubapi.domain.model.Income;
import com.finantialhub.finantialhubapi.domain.model.IncomeFrequency;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = IncomeController.class)
class IncomeControllerTest {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    ObjectMapper objectMapper;

    @MockitoBean
    IncomeService incomeService;

    @Test
    void createIncome_returns201AndBody() throws Exception {
        UUID userId = UUID.randomUUID();

        CreateIncomeRequest request = new CreateIncomeRequest(
                userId.toString(),
                "Salary",
                "MONTHLY",
                new BigDecimal("9000.00"),
                "AUD",
                LocalDate.parse("2026-01-01"),
                null
        );

        Income income = new Income(
                UUID.randomUUID(),
                userId,
                "Salary",
                IncomeFrequency.MONTHLY,
                new BigDecimal("9000.00"),
                "AUD",
                LocalDate.parse("2026-01-01"),
                null,
                Instant.now()
        );

        when(incomeService.createIncome(any())).thenReturn(income);

        mockMvc.perform(post("/api/v1/incomes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(header().exists("Location"))
                .andExpect(jsonPath("$.id").value(income.getId().toString()))
                .andExpect(jsonPath("$.userId").value(userId.toString()))
                .andExpect(jsonPath("$.source").value("Salary"))
                .andExpect(jsonPath("$.frequency").value("MONTHLY"))
                .andExpect(jsonPath("$.amount").value(9000.00))
                .andExpect(jsonPath("$.currency").value("AUD"));
    }

    @Test
    void listIncomes_returns200AndList() throws Exception {
        UUID userId = UUID.randomUUID();

        Income income = new Income(
                UUID.randomUUID(),
                userId,
                "Salary",
                IncomeFrequency.MONTHLY,
                new BigDecimal("9000.00"),
                "AUD",
                LocalDate.parse("2026-01-01"),
                null,
                Instant.now()
        );

        when(incomeService.getIncomesForUser(userId)).thenReturn(List.of(income));

        mockMvc.perform(get("/api/v1/incomes").param("userId", userId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(income.getId().toString()))
                .andExpect(jsonPath("$[0].userId").value(userId.toString()))
                .andExpect(jsonPath("$[0].frequency").value("MONTHLY"));
    }

    record CreateIncomeRequest(
            String userId,
            String source,
            String frequency,
            BigDecimal amount,
            String currency,
            LocalDate startsAt,
            LocalDate endsAt
    ) {}
}
