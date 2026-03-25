package com.finantialhub.finantialhubapi.application.usecases;

import com.finantialhub.finantialhubapi.domain.model.Income;
import com.finantialhub.finantialhubapi.infrastructure.persistence.IncomeRepository;
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
class IncomeServiceTest {

    @Mock
    private IncomeRepository incomeRepository;

    private IncomeService incomeService;

    @BeforeEach
    void setUp() {
        incomeService = new IncomeService(incomeRepository);
    }

    @Test
    void createIncome_savesAndReturnsIncome() {
        IncomeService.CreateIncomeCommand cmd = new IncomeService.CreateIncomeCommand(
                UUID.randomUUID(),
                "Salary",
                "MONTHLY",
                new BigDecimal("5000.00"),
                "AUD",
                LocalDate.now(),
                null
        );

        ArgumentCaptor<Income> captor = ArgumentCaptor.forClass(Income.class);
        when(incomeRepository.save(captor.capture())).thenAnswer(i -> i.getArgument(0));

        Income result = incomeService.createIncome(cmd);

        Income saved = captor.getValue();
        assertThat(saved.getUserId()).isEqualTo(cmd.userId());
        assertThat(saved.getSource()).isEqualTo("Salary");
        assertThat(saved.getAmount()).isEqualByComparingTo("5000.00");
        assertThat(saved.getCurrency()).isEqualTo("AUD");
        assertThat(result).isEqualTo(saved);
    }

    @Test
    void getIncomesForUser_returnsRepositoryResult() {
        UUID userId = UUID.randomUUID();
        when(incomeRepository.findByUserId(userId)).thenReturn(List.of());

        List<Income> result = incomeService.getIncomesForUser(userId);

        assertThat(result).isEmpty();
        verify(incomeRepository).findByUserId(userId);
    }
}
