package com.finantialhub.finantialhubapi.infrastructure.web;

import com.finantialhub.finantialhubapi.application.usecases.ExpenseService;
import com.finantialhub.finantialhubapi.infrastructure.web.dto.ExpenseDtos.*;
import com.finantialhub.finantialhubapi.domain.model.Expense;
import org.springframework.http.ResponseEntity;
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
    public ResponseEntity<ExpenseResponse> createExpense(@RequestBody CreateExpenseRequest request) {
        ExpenseService.CreateExpenseCommand cmd = new ExpenseService.CreateExpenseCommand(
                UUID.fromString(request.userId()),
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
    public List<ExpenseResponse> listExpenses(@RequestParam("userId") UUID userId) {
        return expenseService.getExpensesForUser(userId)
                .stream()
                .map(ExpenseResponse::from)
                .toList();
    }
}
