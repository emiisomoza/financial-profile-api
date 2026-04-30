package com.financialhub.financialhubapi.infrastructure.web.dto;

import com.financialhub.financialhubapi.domain.model.Income;
import org.springframework.lang.Nullable;

import java.math.BigDecimal;
import java.time.LocalDate;

public class IncomeDtos {
    public record CreateIncomeRequest(
            @Nullable String userId,
            String source,
            String frequency,
            BigDecimal amount,
            String currency,
            LocalDate startsAt,
            LocalDate endsAt
    ) {}

    public record UpdateIncomeRequest(
            String source,
            String frequency,
            BigDecimal amount,
            String currency,
            LocalDate startsAt,
            LocalDate endsAt
    ) {}

    public record IncomeResponse(
            String id,
            String userId,
            String source,
            String frequency,
            BigDecimal amount,
            String currency,
            LocalDate startsAt,
            LocalDate endsAt
    ) {
        public static IncomeResponse from(Income income) {
            return new IncomeResponse(
                    income.getId().toString(),
                    income.getUserId().toString(),
                    income.getSource(),
                    income.getFrequency().name(),
                    income.getAmount(),
                    income.getCurrency(),
                    income.getStartsAt(),
                    income.getEndsAt()
            );
        }
    }
}
