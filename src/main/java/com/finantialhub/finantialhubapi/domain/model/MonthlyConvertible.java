package com.finantialhub.finantialhubapi.domain.model;

import java.math.BigDecimal;

public interface MonthlyConvertible {
    BigDecimal toMonthly(BigDecimal amount);
}

