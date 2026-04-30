package com.financialhub.financialhubapi.application.usecases;

import com.financialhub.financialhubapi.domain.exceptions.ExpenseNotFoundException;
import com.financialhub.financialhubapi.domain.model.Expense;
import com.financialhub.financialhubapi.domain.model.ExpenseCategory;
import com.financialhub.financialhubapi.domain.model.ExpenseFrequency;
import com.financialhub.financialhubapi.infrastructure.persistence.ExpenseRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Service
public class ExpenseService {

    private final ExpenseRepository expenseRepository;

    public ExpenseService(ExpenseRepository expenseRepository) {
        this.expenseRepository = expenseRepository;
    }

    public Expense createExpense(CreateExpenseCommand cmd) {
        Expense expense = Expense.createNew(
                cmd.userId(),
                ExpenseCategory.valueOf(cmd.category()),
                cmd.description(),
                cmd.frequency(),
                cmd.amount(),
                cmd.currency(),
                cmd.startsAt(),
                cmd.endsAt()
        );
        return expenseRepository.save(expense);
    }

    public Expense getExpenseById(UUID expenseId) {
        return expenseRepository.findById(expenseId)
                .orElseThrow(() -> new ExpenseNotFoundException(expenseId));
    }

    public List<Expense> getExpensesForUser(UUID userId) {
        return expenseRepository.findByUserId(userId);
    }

    public Expense updateExpense(UUID expenseId, UpdateExpenseCommand cmd) {
        Expense existing = expenseRepository.findById(expenseId)
                .orElseThrow(() -> new ExpenseNotFoundException(expenseId));
        Expense updated = existing.update(
                ExpenseCategory.valueOf(cmd.category()),
                cmd.description(),
                cmd.frequency(),
                cmd.amount(),
                cmd.currency(),
                cmd.startsAt(),
                cmd.endsAt()
        );
        return expenseRepository.save(updated);
    }

    public void deleteExpense(UUID expenseId) {
        if (!expenseRepository.existsById(expenseId)) {
            throw new ExpenseNotFoundException(expenseId);
        }
        expenseRepository.deleteById(expenseId);
    }

    public record CreateExpenseCommand(
            UUID userId,
            String category,
            String description,
            ExpenseFrequency frequency,
            BigDecimal amount,
            String currency,
            LocalDate startsAt,
            LocalDate endsAt
    ) {}

    public record UpdateExpenseCommand(
            String category,
            String description,
            ExpenseFrequency frequency,
            BigDecimal amount,
            String currency,
            LocalDate startsAt,
            LocalDate endsAt
    ) {}
}
