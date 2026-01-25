package com.finantialhub.finantialhubapi.infrastructure.web.dto;

import com.finantialhub.finantialhubapi.domain.model.Income;

import java.math.BigDecimal;
import java.time.LocalDate;

public class IncomeDtos {
    public record CreateIncomeRequest(
            String userId,
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
