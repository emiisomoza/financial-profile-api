package com.finantialhub.finantialhubapi.infrastructure.web.dto;

import com.finantialhub.finantialhubapi.domain.model.Expense;
import com.finantialhub.finantialhubapi.domain.model.ExpenseFrequency;

import java.math.BigDecimal;
import java.time.LocalDate;

public class ExpenseDtos {
    public record CreateExpenseRequest(
            String userId,
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
