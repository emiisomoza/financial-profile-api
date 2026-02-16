package com.finantialhub.finantialhubapi.application.usecases;

import com.finantialhub.finantialhubapi.domain.model.*;
import com.finantialhub.finantialhubapi.infrastructure.persistence.AssetRepository;
import com.finantialhub.finantialhubapi.infrastructure.persistence.ExpenseRepository;
import com.finantialhub.finantialhubapi.infrastructure.persistence.IncomeRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

class SummaryServiceTest {

    private AssetRepository assetRepository;
    private IncomeRepository incomeRepository;
    private ExpenseRepository expenseRepository;

    private SummaryService summaryService;

    @BeforeEach
    void setUp() {
        assetRepository = mock(AssetRepository.class);
        incomeRepository = mock(IncomeRepository.class);
        expenseRepository = mock(ExpenseRepository.class);

        summaryService = new SummaryService(assetRepository, incomeRepository, expenseRepository);
    }

    @Test
    void summary_withMultipleAssetsIncomesExpenses_calculatesTotalsAndSavingsRate() {
        UUID userId = UUID.randomUUID();
        Instant now = Instant.now();

        // Assets: 3 manual (AUD) + 2 market (AUD, unpriced) + 1 manual (USD, ignored)
        List<Asset> assets = List.of(
                assetManual(userId, "AUD", new BigDecimal("1"), new BigDecimal("750000"), now),  // 750000
                assetManual(userId, "AUD", new BigDecimal("2"), new BigDecimal("15000"), now),   // 30000
                assetManual(userId, "AUD", new BigDecimal("1"), new BigDecimal("2000"), now),    // 2000
                assetMarket(userId, "AUD", new BigDecimal("0.5"), now),                           // unpriced
                assetMarket(userId, "AUD", new BigDecimal("10"), now),                            // unpriced
                assetManual(userId, "USD", new BigDecimal("1"), new BigDecimal("999"), now)       // ignored (currency)
        );

        // Incomes: monthly 9000 + weekly 1000 => 1000*52/12 = 4333.333333...
        // ignore one_time for monthly recurring
        List<Income> incomes = List.of(
                income(userId, "AUD", IncomeFrequency.MONTHLY, new BigDecimal("9000.00"), activeStart("2026-01-01"), null, now),
                income(userId, "AUD", IncomeFrequency.WEEKLY, new BigDecimal("1000.00"), activeStart("2026-01-01"), null, now),
                income(userId, "AUD", IncomeFrequency.ONE_TIME, new BigDecimal("5000.00"), activeStart("2026-01-01"), activeEnd("2026-01-01"), now),
                income(userId, "USD", IncomeFrequency.MONTHLY, new BigDecimal("999.00"), activeStart("2026-01-01"), null, now) // ignored currency
        );

        // Expenses: monthly 2400 + fortnightly 600 => 600*26/12 = 1300
        List<Expense> expenses = List.of(
                expense(userId, "AUD", ExpenseFrequency.MONTHLY, new BigDecimal("2400.00"), activeStart("2026-01-01"), null, now),
                expense(userId, "AUD", ExpenseFrequency.FORTNIGHTLY, new BigDecimal("600.00"), activeStart("2026-01-01"), null, now),
                expense(userId, "USD", ExpenseFrequency.MONTHLY, new BigDecimal("50.00"), activeStart("2026-01-01"), null, now) // ignored currency
        );

        when(assetRepository.findByUserId(userId)).thenReturn(assets);
        when(incomeRepository.findByUserId(userId)).thenReturn(incomes);
        when(expenseRepository.findByUserId(userId)).thenReturn(expenses);

        SummaryService.Summary s = summaryService.getSummary(userId);

        // Assets total = 750000 + 30000 + 2000 = 782000
        assertEquals(new BigDecimal("782000"), s.totalAssetsValue());

        // Monthly income = 9000 + 4333.333333 = 13333.333333
        assertBigDecimalEquals("13333.333333", s.monthlyIncome());

        // Monthly expenses = 2400 + 1300 = 3700.000000
        assertBigDecimalEquals("3700.000000", s.monthlyExpenses());

        // Savings = 9633.333333
        assertBigDecimalEquals("9633.333333", s.monthlySavings());

        // Savings rate = savings / income = 0.7225...
        // exact depends on BigDecimal scale; we assert it’s close
        assertTrue(s.savingsRate().compareTo(new BigDecimal("0.72")) > 0);
        assertTrue(s.savingsRate().compareTo(new BigDecimal("0.73")) < 0);

        assertEquals(2, s.unpricedAssetsCount());
        assertEquals("AUD", s.currency());
        assertEquals(userId, s.userId());
    }

    @Test
    void summary_incomeZero_setsSavingsRateToZero_avoidsDivideByZero() {
        UUID userId = UUID.randomUUID();
        Instant now = Instant.now();

        when(assetRepository.findByUserId(userId)).thenReturn(List.of());
        when(incomeRepository.findByUserId(userId)).thenReturn(List.of()); // no income
        when(expenseRepository.findByUserId(userId)).thenReturn(List.of(
                expense(userId, "AUD", ExpenseFrequency.MONTHLY, new BigDecimal("100.00"), activeStart("2026-01-01"), null, now)
        ));

        SummaryService.Summary s = summaryService.getSummary(userId);

        assertBigDecimalEquals("0", s.monthlyIncome());
        assertBigDecimalEquals("100.000000", s.monthlyExpenses());
        assertBigDecimalEquals("-100.000000", s.monthlySavings());
        assertBigDecimalEquals("0", s.savingsRate());
    }

    @Test
    void summary_filtersInactiveIncomesAndExpenses_byDates() {
        UUID userId = UUID.randomUUID();
        Instant now = Instant.now();

        // Active: starts in past, no end
        Income activeIncome = income(userId, "AUD", IncomeFrequency.MONTHLY, new BigDecimal("9000.00"),
                LocalDate.now().minusDays(10), null, now);

        // Inactive: starts in future
        Income futureIncome = income(userId, "AUD", IncomeFrequency.MONTHLY, new BigDecimal("5000.00"),
                LocalDate.now().plusDays(10), null, now);

        // Inactive: ended in past
        Expense endedExpense = expense(userId, "AUD", ExpenseFrequency.MONTHLY, new BigDecimal("100.00"),
                LocalDate.now().minusMonths(2), LocalDate.now().minusMonths(1), now);

        // Active expense
        Expense activeExpense = expense(userId, "AUD", ExpenseFrequency.MONTHLY, new BigDecimal("2400.00"),
                LocalDate.now().minusDays(1), null, now);

        when(assetRepository.findByUserId(userId)).thenReturn(List.of());
        when(incomeRepository.findByUserId(userId)).thenReturn(List.of(activeIncome, futureIncome));
        when(expenseRepository.findByUserId(userId)).thenReturn(List.of(endedExpense, activeExpense));

        SummaryService.Summary s = summaryService.getSummary(userId);

        assertBigDecimalEquals("9000.000000", s.monthlyIncome());
        assertBigDecimalEquals("2400.000000", s.monthlyExpenses());
    }

    @Test
    void summary_includesYearlyAndFortnightlyConversions_correctly() {
        UUID userId = UUID.randomUUID();
        Instant now = Instant.now();

        // Yearly 120000 => monthly 10000
        Income yearly = income(userId, "AUD", IncomeFrequency.YEARLY, new BigDecimal("120000.00"),
                activeStart("2026-01-01"), null, now);

        // Fortnightly 2000 => 2000*26/12 = 4333.333333
        Income fortnightly = income(userId, "AUD", IncomeFrequency.FORTNIGHTLY, new BigDecimal("2000.00"),
                activeStart("2026-01-01"), null, now);

        when(assetRepository.findByUserId(userId)).thenReturn(List.of());
        when(incomeRepository.findByUserId(userId)).thenReturn(List.of(yearly, fortnightly));
        when(expenseRepository.findByUserId(userId)).thenReturn(List.of());

        SummaryService.Summary s = summaryService.getSummary(userId);

        assertBigDecimalEquals("14333.333333", s.monthlyIncome());
    }

    @Test
    void summary_assets_manualOnly_areValued_marketAssetsCountedAsUnpriced() {
        UUID userId = UUID.randomUUID();
        Instant now = Instant.now();

        List<Asset> assets = List.of(
                assetManual(userId, "AUD", new BigDecimal("3"), new BigDecimal("10"), now), // 30
                assetMarket(userId, "AUD", new BigDecimal("1"), now),                       // unpriced
                assetMarket(userId, "AUD", new BigDecimal("2"), now)                        // unpriced
        );

        when(assetRepository.findByUserId(userId)).thenReturn(assets);
        when(incomeRepository.findByUserId(userId)).thenReturn(List.of());
        when(expenseRepository.findByUserId(userId)).thenReturn(List.of());

        SummaryService.Summary s = summaryService.getSummary(userId);

        assertEquals(new BigDecimal("30"), s.totalAssetsValue());
        assertEquals(2, s.unpricedAssetsCount());
    }

    // ===== helpers =====

    private static LocalDate activeStart(String isoDate) {
        return LocalDate.parse(isoDate);
    }

    private static LocalDate activeEnd(String isoDate) {
        return LocalDate.parse(isoDate);
    }

    private static Asset assetManual(UUID userId, String currency, BigDecimal qty, BigDecimal unitValue, Instant createdAt) {
        // Adjust constructor args if yours differs.
        return new Asset(
                UUID.randomUUID(),
                userId,
                AssetType.PROPERTY,
                "Manual Asset",
                null,
                qty,
                ValuationMode.MANUAL,
                unitValue,
                currency,
                createdAt
        );
    }

    private static Asset assetMarket(UUID userId, String currency, BigDecimal qty, Instant createdAt) {
        return new Asset(
                UUID.randomUUID(),
                userId,
                AssetType.CRYPTO,
                "Market Asset",
                "BTCUSDT",
                qty,
                ValuationMode.MARKET,
                null,
                currency,
                createdAt
        );
    }

    private static Income income(UUID userId,
                                 String currency,
                                 IncomeFrequency frequency,
                                 BigDecimal amount,
                                 LocalDate startsAt,
                                 LocalDate endsAt,
                                 Instant createdAt) {
        // Requires public constructor in Income (like Asset) — you updated it earlier.
        return new Income(
                UUID.randomUUID(),
                userId,
                "Salary",
                frequency,
                amount,
                currency,
                startsAt,
                endsAt,
                createdAt
        );
    }

    private static Expense expense(UUID userId,
                                   String currency,
                                   ExpenseFrequency frequency,
                                   BigDecimal amount,
                                   LocalDate startsAt,
                                   LocalDate endsAt,
                                   Instant createdAt) {
        // Requires public constructor in Expense (like Asset) — you added this pattern.
        return new Expense(
                UUID.randomUUID(),
                userId,
                ExpenseCategory.OTHER,
                "Expense",
                frequency,
                amount,
                currency,
                startsAt,
                endsAt,
                createdAt
        );
    }

    private static void assertBigDecimalEquals(String expected, BigDecimal actual) {
        // Compare numerically (ignores scale differences)
        assertEquals(0, new BigDecimal(expected).compareTo(actual),
                () -> "Expected: " + expected + " but was: " + actual);
    }
}
