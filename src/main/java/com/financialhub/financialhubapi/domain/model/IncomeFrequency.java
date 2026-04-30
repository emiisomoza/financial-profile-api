package com.financialhub.financialhubapi.domain.model;

import java.math.BigDecimal;
import java.math.RoundingMode;

public enum IncomeFrequency implements MonthlyConvertible {
    MONTHLY {
        @Override public BigDecimal toMonthly(BigDecimal amount) { return amount; }
    },
    WEEKLY {
        @Override public BigDecimal toMonthly(BigDecimal amount) { return amount.multiply(BigDecimal.valueOf(52))
                .divide(BigDecimal.valueOf(12), 6, RoundingMode.HALF_UP); }
    },
    FORTNIGHTLY {
        @Override public BigDecimal toMonthly(BigDecimal amount) { return amount.multiply(BigDecimal.valueOf(26))
                .divide(BigDecimal.valueOf(12), 6, RoundingMode.HALF_UP); }
    },
    YEARLY {
        @Override public BigDecimal toMonthly(BigDecimal amount) { return amount
                .divide(BigDecimal.valueOf(12), 6, RoundingMode.HALF_UP); }
    },
    ONE_TIME {
        @Override public BigDecimal toMonthly(BigDecimal amount) { return BigDecimal.ZERO; }
    };

    public static IncomeFrequency fromString(String value) {
        return IncomeFrequency.valueOf(value.trim().toUpperCase());
    }
}

