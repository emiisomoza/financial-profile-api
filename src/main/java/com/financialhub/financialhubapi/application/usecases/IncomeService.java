package com.financialhub.financialhubapi.application.usecases;

import com.financialhub.financialhubapi.domain.exceptions.IncomeNotFoundException;
import com.financialhub.financialhubapi.domain.model.Income;
import com.financialhub.financialhubapi.domain.model.IncomeFrequency;
import com.financialhub.financialhubapi.infrastructure.persistence.IncomeRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Service
public class IncomeService {

    private final IncomeRepository incomeRepository;

    public IncomeService(IncomeRepository incomeRepository) {
        this.incomeRepository = incomeRepository;
    }

    public Income createIncome(CreateIncomeCommand cmd) {
        Income income = Income.createNew(
                cmd.userId(),
                cmd.source(),
                IncomeFrequency.valueOf(cmd.frequency()),
                cmd.amount(),
                cmd.currency(),
                cmd.startsAt(),
                cmd.endsAt()
        );
        return incomeRepository.save(income);
    }

    public Income getIncomeById(UUID incomeId) {
        return incomeRepository.findById(incomeId)
                .orElseThrow(() -> new IncomeNotFoundException(incomeId));
    }

    public List<Income> getIncomesForUser(UUID userId) {
        return incomeRepository.findByUserId(userId);
    }

    public Income updateIncome(UUID incomeId, UpdateIncomeCommand cmd) {
        Income existing = incomeRepository.findById(incomeId)
                .orElseThrow(() -> new IncomeNotFoundException(incomeId));
        Income updated = existing.update(
                cmd.source(),
                IncomeFrequency.valueOf(cmd.frequency()),
                cmd.amount(),
                cmd.currency(),
                cmd.startsAt(),
                cmd.endsAt()
        );
        return incomeRepository.save(updated);
    }

    public void deleteIncome(UUID incomeId) {
        if (!incomeRepository.existsById(incomeId)) {
            throw new IncomeNotFoundException(incomeId);
        }
        incomeRepository.deleteById(incomeId);
    }

    public record CreateIncomeCommand(
            UUID userId,
            String source,
            String frequency,
            BigDecimal amount,
            String currency,
            LocalDate startsAt,
            LocalDate endsAt
    ) {}

    public record UpdateIncomeCommand(
            String source,
            String frequency,
            BigDecimal amount,
            String currency,
            LocalDate startsAt,
            LocalDate endsAt
    ) {}
}
