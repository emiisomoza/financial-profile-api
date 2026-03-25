package com.finantialhub.finantialhubapi.application.usecases;

import com.finantialhub.finantialhubapi.domain.model.Expense;
import com.finantialhub.finantialhubapi.domain.model.ExpenseFrequency;
import com.finantialhub.finantialhubapi.infrastructure.persistence.ExpenseRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ExpenseServiceTest {

    @Mock
    private ExpenseRepository expenseRepository;

    private ExpenseService expenseService;

    @BeforeEach
    void setUp() {
        expenseService = new ExpenseService(expenseRepository);
    }

    @Test
    void createExpense_savesAndReturnsExpense() {
        ExpenseService.CreateExpenseCommand cmd = new ExpenseService.CreateExpenseCommand(
                UUID.randomUUID(),
                "RENT",
                "Monthly rent",
                ExpenseFrequency.MONTHLY,
                new BigDecimal("1500.00"),
                "AUD",
                LocalDate.now(),
                null
        );

        ArgumentCaptor<Expense> captor = ArgumentCaptor.forClass(Expense.class);
        when(expenseRepository.save(captor.capture())).thenAnswer(i -> i.getArgument(0));

        Expense result = expenseService.createExpense(cmd);

        Expense saved = captor.getValue();
        assertThat(saved.getUserId()).isEqualTo(cmd.userId());
        assertThat(saved.getDescription()).isEqualTo("Monthly rent");
        assertThat(saved.getAmount()).isEqualByComparingTo("1500.00");
        assertThat(saved.getCurrency()).isEqualTo("AUD");
        assertThat(result).isEqualTo(saved);
    }

    @Test
    void getExpensesForUser_returnsRepositoryResult() {
        UUID userId = UUID.randomUUID();
        when(expenseRepository.findByUserId(userId)).thenReturn(List.of());

        List<Expense> result = expenseService.getExpensesForUser(userId);

        assertThat(result).isEmpty();
        verify(expenseRepository).findByUserId(userId);
    }
}
