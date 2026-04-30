package com.financialhub.financialhubapi.infrastructure.web.dto;

import com.financialhub.financialhubapi.application.usecases.SummaryService;
import java.math.BigDecimal;

public class SummaryDtos {
    public record SummaryResponse(
            String userId,
            String currency,
            BigDecimal totalAssetsValue,
            BigDecimal monthlyIncome,
            BigDecimal monthlyExpenses,
            BigDecimal monthlySavings,
            BigDecimal savingsRate,
            int unpricedAssetsCount
    ) {
        public static SummaryResponse from(SummaryService.Summary s) {
            return new SummaryResponse(
                    s.userId().toString(),
                    s.currency(),
                    s.totalAssetsValue(),
                    s.monthlyIncome(),
                    s.monthlyExpenses(),
                    s.monthlySavings(),
                    s.savingsRate(),
                    s.unpricedAssetsCount()
            );
        }
    }
}
