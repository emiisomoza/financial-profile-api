package com.finantialhub.finantialhubapi.domain.exceptions;

import java.util.UUID;

public class ExpenseNotFoundException extends RuntimeException {
    public ExpenseNotFoundException(UUID expenseId) {
        super("Expense not found: " + expenseId);
    }
}
