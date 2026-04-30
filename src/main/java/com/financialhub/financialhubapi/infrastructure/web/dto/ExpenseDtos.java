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

    public record UpdateExpenseRequest(
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
