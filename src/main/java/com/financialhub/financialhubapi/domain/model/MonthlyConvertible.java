package com.financialhub.financialhubapi.domain.model;

import java.math.BigDecimal;

public interface MonthlyConvertible {
    BigDecimal toMonthly(BigDecimal amount);
}

