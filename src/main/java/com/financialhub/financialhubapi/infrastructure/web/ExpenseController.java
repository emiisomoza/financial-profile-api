package com.financialhub.financialhubapi.infrastructure.web;

import com.financialhub.financialhubapi.application.usecases.ExpenseService;
import com.financialhub.financialhubapi.infrastructure.security.SecurityUtils;
import com.financialhub.financialhubapi.infrastructure.web.dto.ExpenseDtos.*;
import com.financialhub.financialhubapi.domain.model.Expense;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/expenses")
public class ExpenseController {

    private final ExpenseService expenseService;

    public ExpenseController(ExpenseService expenseService) {
        this.expenseService = expenseService;
    }

    @PostMapping
    public ResponseEntity<ExpenseResponse> createExpense(
            @AuthenticationPrincipal Jwt jwt,
            @RequestBody CreateExpenseRequest request) {

        UUID userId = SecurityUtils.resolveUserId(jwt,
                request.userId() != null ? UUID.fromString(request.userId()) : null);

        ExpenseService.CreateExpenseCommand cmd = new ExpenseService.CreateExpenseCommand(
                userId,
                request.category(),
                request.description(),
                request.frequency(),
                request.amount(),
                request.currency(),
                request.startsAt(),
                request.endsAt()
        );

        Expense expense = expenseService.createExpense(cmd);
        URI location = URI.create("/api/v1/expenses/" + expense.getId());
        return ResponseEntity.created(location).body(ExpenseResponse.from(expense));
    }

    @GetMapping
    public List<ExpenseResponse> listExpenses(
            @AuthenticationPrincipal Jwt jwt,
            @RequestParam(required = false) UUID userId) {

        UUID resolvedId = SecurityUtils.resolveUserId(jwt, userId);
        return expenseService.getExpensesForUser(resolvedId)
                .stream()
                .map(ExpenseResponse::from)
                .toList();
    }

    @PutMapping("/{id}")
    public ResponseEntity<ExpenseResponse> updateExpense(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID id,
            @RequestBody UpdateExpenseRequest request) {

        Expense existing = expenseService.getExpenseById(id);
        SecurityUtils.requireOwnership(jwt, existing.getUserId());

        ExpenseService.UpdateExpenseCommand cmd = new ExpenseService.UpdateExpenseCommand(
                request.category(),
                request.description(),
                request.frequency(),
                request.amount(),
                request.currency(),
                request.startsAt(),
                request.endsAt()
        );

        Expense updated = expenseService.updateExpense(id, cmd);
        return ResponseEntity.ok(ExpenseResponse.from(updated));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteExpense(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID id) {

        Expense existing = expenseService.getExpenseById(id);
        SecurityUtils.requireOwnership(jwt, existing.getUserId());

        expenseService.deleteExpense(id);
        return ResponseEntity.noContent().build();
    }
}
