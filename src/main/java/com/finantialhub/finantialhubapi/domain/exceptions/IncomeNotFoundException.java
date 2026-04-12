package com.finantialhub.finantialhubapi.domain.exceptions;

import java.util.UUID;

public class IncomeNotFoundException extends RuntimeException {
    public IncomeNotFoundException(UUID incomeId) {
        super("Income not found: " + incomeId);
    }
}
