package com.finantialhub.finantialhubapi.application.usecases;

import com.finantialhub.finantialhubapi.domain.model.*;
import com.finantialhub.finantialhubapi.domain.ports.MarketPricePort;
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
    private MarketPricePort marketPricePort;

    private SummaryService summaryService;

    private static final String AUD = "AUD";

    @BeforeEach
    void setUp() {
        assetRepository = mock(AssetRepository.class);
        incomeRepository = mock(IncomeRepository.class);
        expenseRepository = mock(ExpenseRepository.class);
        marketPricePort = mock(MarketPricePort.class);

        summaryService = new SummaryService(
                assetRepository,
                incomeRepository,
                expenseRepository,
                marketPricePort
        );
    }

    @Test
    void summary_withMultipleAssetsIncomesExpenses_calculatesTotalsAndSavingsRate() {
        UUID userId = UUID.randomUUID();
        Instant now = Instant.now();

        // Assets:
        //   3 manual AUD: 750000 + 30000 + 2000 = 782000
        //   1 market CRYPTO BTC: 0.5 BTC × 98000 AUD = 49000
        //   1 market STOCK AAPL: 10 × 278 AUD = 2780
        //   1 manual USD: 1000 USD × FX(1.55) = 1550 AUD
        List<Asset> assets = List.of(
                assetManual(userId, AUD, new BigDecimal("1"), new BigDecimal("750000"), now),
                assetManual(userId, AUD, new BigDecimal("2"), new BigDecimal("15000"), now),
                assetManual(userId, AUD, new BigDecimal("1"), new BigDecimal("2000"), now),
                assetMarketCrypto(userId, AUD, new BigDecimal("0.5"), "BTC", now),
                assetMarketStock(userId, AUD, new BigDecimal("10"), "AAPL", now),
                assetManualUsd(userId, "USD", new BigDecimal("1"), new BigDecimal("1000"), now)
        );

        List<Income> incomes = List.of(
                income(userId, AUD, IncomeFrequency.MONTHLY, new BigDecimal("9000.00"), activeStart("2026-01-01"), null, now),
                income(userId, AUD, IncomeFrequency.WEEKLY, new BigDecimal("1000.00"), activeStart("2026-01-01"), null, now),
                income(userId, AUD, IncomeFrequency.ONE_TIME, new BigDecimal("5000.00"), activeStart("2026-01-01"), activeEnd("2026-01-01"), now),
                income(userId, "USD", IncomeFrequency.MONTHLY, new BigDecimal("999.00"), activeStart("2026-01-01"), null, now)
        );

        List<Expense> expenses = List.of(
                expense(userId, AUD, ExpenseFrequency.MONTHLY, new BigDecimal("2400.00"), activeStart("2026-01-01"), null, now),
                expense(userId, AUD, ExpenseFrequency.FORTNIGHTLY, new BigDecimal("600.00"), activeStart("2026-01-01"), null, now),
                expense(userId, "USD", ExpenseFrequency.MONTHLY, new BigDecimal("50.00"), activeStart("2026-01-01"), null, now)
        );

        when(assetRepository.findByUserId(userId)).thenReturn(assets);
        when(incomeRepository.findByUserId(userId)).thenReturn(incomes);
        when(expenseRepository.findByUserId(userId)).thenReturn(expenses);

        // Mock market prices
        when(marketPricePort.getPrice("crypto", "BTC", AUD)).thenReturn(new BigDecimal("98000"));
        when(marketPricePort.getPrice("stock", "AAPL", AUD)).thenReturn(new BigDecimal("278"));
        when(marketPricePort.getPrice("fx", "USD", AUD)).thenReturn(new BigDecimal("1.55"));

        SummaryService.Summary s = summaryService.getSummary(userId, AUD);

        // 750000 + 30000 + 2000 + 49000 + 2780 + 1550 = 835330
        assertBigDecimalEquals("835330.00", s.totalAssetsValue());

        // Monthly income = 9000 + 4333.333333 + (999 × 1.55) = 9000 + 4333.333333 + 1548.45 = 14881.783333
        assertBigDecimalEquals("14881.783333", s.monthlyIncome());

        // Monthly expenses = 2400 + 1300 + (50 × 1.55) = 3700 + 77.50 = 3777.500000
        assertBigDecimalEquals("3777.500000", s.monthlyExpenses());

        // Savings = 11104.283333
        assertBigDecimalEquals("11104.283333", s.monthlySavings());

        assertEquals(new BigDecimal("0.746166"), s.savingsRate());

        assertEquals(0, s.unpricedAssetsCount());
        assertEquals(AUD, s.currency());
        assertEquals(userId, s.userId());
    }

    @Test
    void summary_marketAsset_withNullSymbol_countsAsUnpriced() {
        UUID userId = UUID.randomUUID();
        Instant now = Instant.now();

        List<Asset> assets = List.of(
                assetMarketNoSymbol(userId, AUD, new BigDecimal("1"), now)
        );

        when(assetRepository.findByUserId(userId)).thenReturn(assets);
        when(incomeRepository.findByUserId(userId)).thenReturn(List.of());
        when(expenseRepository.findByUserId(userId)).thenReturn(List.of());

        SummaryService.Summary s = summaryService.getSummary(userId, AUD);

        assertEquals(1, s.unpricedAssetsCount());
        assertBigDecimalEquals("0", s.totalAssetsValue());
        verifyNoInteractions(marketPricePort);
    }

    @Test
    void summary_marketAsset_whenPriceApiFails_countsAsUnpriced() {
        UUID userId = UUID.randomUUID();
        Instant now = Instant.now();

        List<Asset> assets = List.of(
                assetMarketCrypto(userId, AUD, new BigDecimal("1"), "BTC", now)
        );

        when(assetRepository.findByUserId(userId)).thenReturn(assets);
        when(incomeRepository.findByUserId(userId)).thenReturn(List.of());
        when(expenseRepository.findByUserId(userId)).thenReturn(List.of());
        when(marketPricePort.getPrice("crypto", "BTC", AUD))
                .thenThrow(new RuntimeException("Price API unavailable"));

        SummaryService.Summary s = summaryService.getSummary(userId, AUD);

        assertEquals(1, s.unpricedAssetsCount());
        assertBigDecimalEquals("0", s.totalAssetsValue());
    }

    @Test
    void summary_manualAsset_differentCurrency_convertedViaFx() {
        UUID userId = UUID.randomUUID();
        Instant now = Instant.now();

        // 1000 USD × 1.55 = 1550 AUD
        List<Asset> assets = List.of(
                assetManualUsd(userId, "USD", new BigDecimal("1"), new BigDecimal("1000"), now)
        );

        when(assetRepository.findByUserId(userId)).thenReturn(assets);
        when(incomeRepository.findByUserId(userId)).thenReturn(List.of());
        when(expenseRepository.findByUserId(userId)).thenReturn(List.of());
        when(marketPricePort.getPrice("fx", "USD", AUD)).thenReturn(new BigDecimal("1.55"));

        SummaryService.Summary s = summaryService.getSummary(userId, AUD);

        assertBigDecimalEquals("1550.00", s.totalAssetsValue());
        assertEquals(0, s.unpricedAssetsCount());
        verify(marketPricePort).getPrice("fx", "USD", AUD);
    }

    @Test
    void summary_manualAsset_sameCurrency_doesNotCallPriceApi() {
        UUID userId = UUID.randomUUID();
        Instant now = Instant.now();

        List<Asset> assets = List.of(
                assetManual(userId, AUD, new BigDecimal("1"), new BigDecimal("50000"), now)
        );

        when(assetRepository.findByUserId(userId)).thenReturn(assets);
        when(incomeRepository.findByUserId(userId)).thenReturn(List.of());
        when(expenseRepository.findByUserId(userId)).thenReturn(List.of());

        SummaryService.Summary s = summaryService.getSummary(userId, AUD);

        assertBigDecimalEquals("50000", s.totalAssetsValue());
        verifyNoInteractions(marketPricePort);
    }

    @Test
    void summary_incomeZero_setsSavingsRateToZero_avoidsDivideByZero() {
        UUID userId = UUID.randomUUID();
        Instant now = Instant.now();

        when(assetRepository.findByUserId(userId)).thenReturn(List.of());
        when(incomeRepository.findByUserId(userId)).thenReturn(List.of());
        when(expenseRepository.findByUserId(userId)).thenReturn(List.of(
                expense(userId, AUD, ExpenseFrequency.MONTHLY, new BigDecimal("100.00"), activeStart("2026-01-01"), null, now)
        ));

        SummaryService.Summary s = summaryService.getSummary(userId, AUD);

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
        Income activeIncome = income(userId, AUD, IncomeFrequency.MONTHLY, new BigDecimal("9000.00"),
                LocalDate.now().minusDays(10), null, now);

        // Inactive: starts in future — should be ignored
        Income futureIncome = income(userId, AUD, IncomeFrequency.MONTHLY, new BigDecimal("5000.00"),
                LocalDate.now().plusDays(10), null, now);

        // Active USD income: 1000 USD × 1.55 = 1550 AUD
        Income activeUsdIncome = income(userId, "USD", IncomeFrequency.MONTHLY, new BigDecimal("1000.00"),
                LocalDate.now().minusDays(5), null, now);

        // Inactive USD income: starts in future — should be ignored
        Income futureUsdIncome = income(userId, "USD", IncomeFrequency.MONTHLY, new BigDecimal("9999.00"),
                LocalDate.now().plusDays(1), null, now);

        // Inactive: ended in past — should be ignored
        Expense endedExpense = expense(userId, AUD, ExpenseFrequency.MONTHLY, new BigDecimal("100.00"),
                LocalDate.now().minusMonths(2), LocalDate.now().minusMonths(1), now);

        // Active expense
        Expense activeExpense = expense(userId, AUD, ExpenseFrequency.MONTHLY, new BigDecimal("2400.00"),
                LocalDate.now().minusDays(1), null, now);

        // Active USD expense: 200 USD × 1.55 = 310 AUD
        Expense activeUsdExpense = expense(userId, "USD", ExpenseFrequency.MONTHLY, new BigDecimal("200.00"),
                LocalDate.now().minusDays(1), null, now);

        when(assetRepository.findByUserId(userId)).thenReturn(List.of());
        when(incomeRepository.findByUserId(userId)).thenReturn(
                List.of(activeIncome, futureIncome, activeUsdIncome, futureUsdIncome)
        );
        when(expenseRepository.findByUserId(userId)).thenReturn(
                List.of(endedExpense, activeExpense, activeUsdExpense)
        );
        when(marketPricePort.getPrice("fx", "USD", AUD)).thenReturn(new BigDecimal("1.55"));

        SummaryService.Summary s = summaryService.getSummary(userId, AUD);

        // 9000 + (1000 × 1.55) = 9000 + 1550 = 10550
        assertBigDecimalEquals("10550.000000", s.monthlyIncome());

        // 2400 + (200 × 1.55) = 2400 + 310 = 2710
        assertBigDecimalEquals("2710.000000", s.monthlyExpenses());

        // FX called once for USD income and once for USD expense
        verify(marketPricePort, times(2)).getPrice("fx", "USD", AUD);
    }

    @Test
    void summary_includesYearlyAndFortnightlyConversions_correctly() {
        UUID userId = UUID.randomUUID();
        Instant now = Instant.now();

        Income yearly = income(userId, AUD, IncomeFrequency.YEARLY, new BigDecimal("120000.00"),
                activeStart("2026-01-01"), null, now);
        Income fortnightly = income(userId, AUD, IncomeFrequency.FORTNIGHTLY, new BigDecimal("2000.00"),
                activeStart("2026-01-01"), null, now);

        when(assetRepository.findByUserId(userId)).thenReturn(List.of());
        when(incomeRepository.findByUserId(userId)).thenReturn(List.of(yearly, fortnightly));
        when(expenseRepository.findByUserId(userId)).thenReturn(List.of());

        SummaryService.Summary s = summaryService.getSummary(userId, AUD);

        assertBigDecimalEquals("14333.333333", s.monthlyIncome());
    }

    // ===== Asset helpers =====

    private static Asset assetManual(UUID userId, String currency, BigDecimal qty, BigDecimal unitValue, Instant createdAt) {
        return new Asset(UUID.randomUUID(), userId, AssetType.PROPERTY, "Manual Asset", null,
                qty, ValuationMode.MANUAL, unitValue, currency, createdAt);
    }

    private static Asset assetManualUsd(UUID userId, String currency, BigDecimal qty, BigDecimal unitValue, Instant createdAt) {
        return new Asset(UUID.randomUUID(), userId, AssetType.CASH, "USD Savings", null,
                qty, ValuationMode.MANUAL, unitValue, currency, createdAt);
    }

    private static Asset assetMarketCrypto(UUID userId, String currency, BigDecimal qty, String symbol, Instant createdAt) {
        return new Asset(UUID.randomUUID(), userId, AssetType.CRYPTO, "Crypto Asset", symbol,
                qty, ValuationMode.MARKET, null, currency, createdAt);
    }

    private static Asset assetMarketStock(UUID userId, String currency, BigDecimal qty, String symbol, Instant createdAt) {
        return new Asset(UUID.randomUUID(), userId, AssetType.STOCK, "Stock Asset", symbol,
                qty, ValuationMode.MARKET, null, currency, createdAt);
    }

    private static Asset assetMarketNoSymbol(UUID userId, String currency, BigDecimal qty, Instant createdAt) {
        return new Asset(UUID.randomUUID(), userId, AssetType.CRYPTO, "No Symbol Asset", null,
                qty, ValuationMode.MARKET, null, currency, createdAt);
    }

    // ===== Income / Expense helpers =====

    private static LocalDate activeStart(String isoDate) { return LocalDate.parse(isoDate); }
    private static LocalDate activeEnd(String isoDate) { return LocalDate.parse(isoDate); }

    private static Income income(UUID userId, String currency, IncomeFrequency frequency,
                                 BigDecimal amount, LocalDate startsAt, LocalDate endsAt, Instant createdAt) {
        return new Income(UUID.randomUUID(), userId, "Salary", frequency, amount, currency, startsAt, endsAt, createdAt);
    }

    private static Expense expense(UUID userId, String currency, ExpenseFrequency frequency,
                                   BigDecimal amount, LocalDate startsAt, LocalDate endsAt, Instant createdAt) {
        return new Expense(UUID.randomUUID(), userId, ExpenseCategory.OTHER, "Expense",
                frequency, amount, currency, startsAt, endsAt, createdAt);
    }

    private static void assertBigDecimalEquals(String expected, BigDecimal actual) {
        assertEquals(0, new BigDecimal(expected).compareTo(actual),
                () -> "Expected: " + expected + " but was: " + actual);
    }
}