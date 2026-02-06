package com.finantialhub.finantialhubapi.application.usecases;

import com.finantialhub.finantialhubapi.domain.model.*;
import com.finantialhub.finantialhubapi.infrastructure.persistence.AssetRepository;
import com.finantialhub.finantialhubapi.infrastructure.persistence.ExpenseRepository;
import com.finantialhub.finantialhubapi.infrastructure.persistence.IncomeRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Service
public class SummaryService {

    private final AssetRepository assetRepository;
    private final IncomeRepository incomeRepository;
    private final ExpenseRepository expenseRepository;

    public SummaryService(AssetRepository assetRepository,
                          IncomeRepository incomeRepository,
                          ExpenseRepository expenseRepository) {
        this.assetRepository = assetRepository;
        this.incomeRepository = incomeRepository;
        this.expenseRepository = expenseRepository;
    }

    public Summary getSummary(UUID userId) {
        // Simple v1 rule: summary currency = "AUD" (or detect from user later, also I can convert it to a selected currency with a service)
        String currency = "AUD";

        List<Asset> assets = assetRepository.findByUserId(userId);
        List<Income> incomes = incomeRepository.findByUserId(userId);
        List<Expense> expenses = expenseRepository.findByUserId(userId);

        // Assets valuation (v1: manual only)
        AssetValuationResult assetsVal = calculateAssetsValue(assets, currency);

        BigDecimal monthlyIncome = incomes.stream()
                .filter(i -> currency.equalsIgnoreCase(i.getCurrency()))
                .filter(this::isActiveNow)
                .map(this::toMonthlyAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal monthlyExpenses = expenses.stream()
                .filter(e -> currency.equalsIgnoreCase(e.getCurrency()))
                .filter(this::isActiveNow)
                .map(this::toMonthlyAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal monthlySavings = monthlyIncome.subtract(monthlyExpenses);

        BigDecimal savingsRate = BigDecimal.ZERO;
        if (monthlyIncome.compareTo(BigDecimal.ZERO) > 0) {
            savingsRate = monthlySavings
                    .divide(monthlyIncome, 6, RoundingMode.HALF_UP);
        }

        return new Summary(
                userId,
                currency,
                assetsVal.totalValue,
                monthlyIncome,
                monthlyExpenses,
                monthlySavings,
                savingsRate,
                assetsVal.unpricedCount
        );
    }

    // ====== Helpers ======

    private AssetValuationResult calculateAssetsValue(List<Asset> assets, String currency) {
        BigDecimal total = BigDecimal.ZERO;
        int unpriced = 0;

        for (Asset a : assets) {
            if (!currency.equalsIgnoreCase(a.getCurrency())) {
                continue; // v1: skip other currencies, after I can convert currencies to the selected currency
            }

            // If your Asset has valuationMode + manualUnitValue:
            if (a.getValuationMode() == ValuationMode.MANUAL) {
                BigDecimal unit = a.getManualUnitValue() == null ? BigDecimal.ZERO : a.getManualUnitValue();
                total = total.add(a.getQuantity().multiply(unit));
            } else {
                // MARKET (needs price service later)
                unpriced++;
            }
        }
        return new AssetValuationResult(total, unpriced);
    }

    private boolean isActiveNow(Income income) {
        LocalDate today = LocalDate.now();
        if (income.getStartsAt() != null && today.isBefore(income.getStartsAt())) return false;
        return income.getEndsAt() == null || !today.isAfter(income.getEndsAt());
    }

    private boolean isActiveNow(Expense expense) {
        LocalDate today = LocalDate.now();
        if (expense.getStartsAt() != null && today.isBefore(expense.getStartsAt())) return false;
        return expense.getEndsAt() == null || !today.isAfter(expense.getEndsAt());
    }

    /**
     * Income uses IncomeFrequency enum (recommended).
     */
    private BigDecimal toMonthlyAmount(Income income) {
        BigDecimal amount = income.getAmount();
        IncomeFrequency f = income.getFrequency();
        return switch (f) {
            case MONTHLY -> amount;
            case WEEKLY -> amount.multiply(BigDecimal.valueOf(52)).divide(BigDecimal.valueOf(12), 6, RoundingMode.HALF_UP);
            case FORTNIGHTLY -> amount.multiply(BigDecimal.valueOf(26)).divide(BigDecimal.valueOf(12), 6, RoundingMode.HALF_UP);
            case YEARLY -> amount.divide(BigDecimal.valueOf(12), 6, RoundingMode.HALF_UP);
            case ONE_TIME -> BigDecimal.ZERO; // v1: not included in monthly recurring
        };
    }

    private BigDecimal toMonthlyAmount(Expense expense) {
        BigDecimal amount = expense.getAmount();

        // If your Expense has ExpenseFrequency frequency enum, replace this parse with expense.getFrequency()
        ExpenseFrequency f = ExpenseFrequency.fromString(expense.getFrequency().toString());

        return switch (f) {
            case MONTHLY -> amount;
            case WEEKLY -> amount.multiply(BigDecimal.valueOf(52)).divide(BigDecimal.valueOf(12), 6, RoundingMode.HALF_UP);
            case FORTNIGHTLY -> amount.multiply(BigDecimal.valueOf(26)).divide(BigDecimal.valueOf(12), 6, RoundingMode.HALF_UP);
            case YEARLY -> amount.divide(BigDecimal.valueOf(12), 6, RoundingMode.HALF_UP);
            case ONE_TIME -> BigDecimal.ZERO; // v1: not included in monthly recurring
        };
    }

    private record AssetValuationResult(BigDecimal totalValue, int unpricedCount) {}

    public record Summary(
            UUID userId,
            String currency,
            BigDecimal totalAssetsValue,
            BigDecimal monthlyIncome,
            BigDecimal monthlyExpenses,
            BigDecimal monthlySavings,
            BigDecimal savingsRate,
            int unpricedAssetsCount
    ) {}
}
