package com.finantialhub.finantialhubapi.application.usecases;

import com.finantialhub.finantialhubapi.domain.model.*;
import com.finantialhub.finantialhubapi.domain.ports.MarketPricePort;
import com.finantialhub.finantialhubapi.infrastructure.persistence.AssetRepository;
import com.finantialhub.finantialhubapi.infrastructure.persistence.ExpenseRepository;
import com.finantialhub.finantialhubapi.infrastructure.persistence.IncomeRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class SummaryService {

    private static final Logger log = LoggerFactory.getLogger(SummaryService.class);

    private final AssetRepository assetRepository;
    private final IncomeRepository incomeRepository;
    private final ExpenseRepository expenseRepository;
    private final MarketPricePort marketPricePort;

    public SummaryService(AssetRepository assetRepository,
                          IncomeRepository incomeRepository,
                          ExpenseRepository expenseRepository,
                          MarketPricePort marketPricePort) {
        this.assetRepository = assetRepository;
        this.incomeRepository = incomeRepository;
        this.expenseRepository = expenseRepository;
        this.marketPricePort = marketPricePort;
    }

    public Summary getSummary(UUID userId, String currency) {

        List<Asset> assets = assetRepository.findByUserId(userId);
        List<Income> incomes = incomeRepository.findByUserId(userId);
        List<Expense> expenses = expenseRepository.findByUserId(userId);

        AssetValuationResult assetsVal = calculateAssetsValue(assets, currency);

        BigDecimal monthlyIncome = incomes.stream()
                .filter(this::isActiveNow)
                .map(i -> convertToTargetCurrency(toMonthlyAmount(i), i.getCurrency(), currency))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal monthlyExpenses = expenses.stream()
                .filter(this::isActiveNow)
                .map(e -> convertToTargetCurrency(toMonthlyAmount(e), e.getCurrency(), currency))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal monthlySavings = monthlyIncome.subtract(monthlyExpenses);

        BigDecimal savingsRate = BigDecimal.ZERO;
        if (monthlyIncome.compareTo(BigDecimal.ZERO) > 0) {
            savingsRate = monthlySavings.divide(monthlyIncome, 6, RoundingMode.HALF_UP);
        }

        return new Summary(
                userId,
                currency,
                assetsVal.totalValue(),
                monthlyIncome,
                monthlyExpenses,
                monthlySavings,
                savingsRate,
                assetsVal.unpricedCount()
        );
    }

    // ====== Helpers ======

    private AssetValuationResult calculateAssetsValue(List<Asset> assets, String targetCurrency) {
        BigDecimal total = BigDecimal.ZERO;
        int unpriced = 0;

        for (Asset asset : assets) {
            try {
                Optional<BigDecimal> value = resolveAssetValue(asset, targetCurrency);
                if (value.isPresent()) {
                    total = total.add(value.get());
                } else {
                    unpriced++;
                }
            } catch (Exception e) {
                log.warn("Could not price asset {} ({}): {}", asset.getName(), asset.getSymbol(), e.getMessage());
                unpriced++;
            }
        }

        return new AssetValuationResult(total, unpriced);
    }

    private Optional<BigDecimal> resolveAssetValue(Asset asset, String targetCurrency) {
        return resolveUnitValue(asset, targetCurrency)
                .map(unitValue -> asset.getQuantity().multiply(unitValue));
    }

    private Optional<BigDecimal> resolveUnitValue(Asset asset, String targetCurrency) {
        if (asset.getValuationMode() == ValuationMode.MANUAL) {
            BigDecimal manualValue = asset.getManualUnitValue() == null
                    ? BigDecimal.ZERO
                    : asset.getManualUnitValue();

            if (targetCurrency.equalsIgnoreCase(asset.getCurrency())) {
                return Optional.of(manualValue);
            }

            BigDecimal fxRate = marketPricePort.getPrice("fx", asset.getCurrency(), targetCurrency);
            return Optional.of(manualValue.multiply(fxRate));
        }

        if (asset.getValuationMode() == ValuationMode.MARKET) {
            String priceApiType = asset.getType().toPriceApiAssetType();

            if (priceApiType == null) {
                log.warn("Asset type {} does not support market pricing", asset.getType());
                return Optional.empty();
            }

            if (asset.getSymbol() == null || asset.getSymbol().isBlank()) {
                log.warn("Asset {} has MARKET valuation but no symbol", asset.getName());
                return Optional.empty();
            }

            return Optional.of(marketPricePort.getPrice(priceApiType, asset.getSymbol(), targetCurrency));
        }

        return Optional.empty();
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

    private BigDecimal toMonthlyAmount(Income income) {
        return income.getFrequency().toMonthly(income.getAmount());
    }

    private BigDecimal toMonthlyAmount(Expense expense) {
        return expense.getFrequency().toMonthly(expense.getAmount());
    }

    private record AssetValuationResult(BigDecimal totalValue, int unpricedCount) {}

    private BigDecimal convertToTargetCurrency(BigDecimal amount, String fromCurrency, String toCurrency) {
        if (toCurrency.equalsIgnoreCase(fromCurrency)) {
            return amount;
        }
        BigDecimal fxRate = marketPricePort.getPrice("fx", fromCurrency, toCurrency);
        return amount.multiply(fxRate);
    }

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