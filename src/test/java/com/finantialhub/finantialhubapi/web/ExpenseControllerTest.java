package com.finantialhub.finantialhubapi.web;

import com.finantialhub.finantialhubapi.infrastructure.web.ExpenseController;
import tools.jackson.databind.ObjectMapper;
import com.finantialhub.finantialhubapi.application.usecases.ExpenseService;
import com.finantialhub.finantialhubapi.domain.model.Expense;
import com.finantialhub.finantialhubapi.domain.model.ExpenseCategory;
import com.finantialhub.finantialhubapi.domain.model.ExpenseFrequency;
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

@WebMvcTest(controllers = ExpenseController.class)
class ExpenseControllerTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;

    @MockitoBean ExpenseService expenseService;

    @Test
    void createExpense_returns201AndBody() throws Exception {
        UUID userId = UUID.randomUUID();

        CreateExpenseRequest request = new CreateExpenseRequest(
                userId.toString(),
                "RENT",
                "Rent payment",
                ExpenseFrequency.MONTHLY,
                new BigDecimal("2400.00"),
                "AUD",
                LocalDate.parse("2026-01-01"),
                null
        );

        Expense expense = new Expense(
                UUID.randomUUID(),
                userId,
                ExpenseCategory.RENT,
                "Rent payment",
                ExpenseFrequency.MONTHLY,
                new BigDecimal("2400.00"),
                "AUD",
                LocalDate.parse("2026-01-01"),
                null,
                Instant.now()
        );

        when(expenseService.createExpense(any())).thenReturn(expense);

        mockMvc.perform(post("/api/v1/expenses")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(header().exists("Location"))
                .andExpect(jsonPath("$.userId").value(userId.toString()))
                .andExpect(jsonPath("$.category").value("RENT"))
                .andExpect(jsonPath("$.amount").value(2400.00));
    }

    @Test
    void listExpenses_returns200AndList() throws Exception {
        UUID userId = UUID.randomUUID();

        Expense expense = new Expense(
                UUID.randomUUID(),
                userId,
                ExpenseCategory.GROCERIES,
                "Weekly groceries",
                ExpenseFrequency.WEEKLY,
                new BigDecimal("220.00"),
                "AUD",
                LocalDate.parse("2026-01-01"),
                null,
                Instant.now()
        );

        when(expenseService.getExpensesForUser(userId)).thenReturn(List.of(expense));

        mockMvc.perform(get("/api/v1/expenses").param("userId", userId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].userId").value(userId.toString()))
                .andExpect(jsonPath("$[0].category").value("GROCERIES"));
    }

    record CreateExpenseRequest(
            String userId,
            String category,
            String description,
            ExpenseFrequency frequency,
            BigDecimal amount,
            String currency,
            LocalDate startsAt,
            LocalDate endsAt
    ) {}
}
