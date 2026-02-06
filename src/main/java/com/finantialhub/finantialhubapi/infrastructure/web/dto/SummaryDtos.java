package com.finantialhub.finantialhubapi.infrastructure.web.dto;

import com.finantialhub.finantialhubapi.application.usecases.SummaryService;

public class SummaryDtos {
    public record SummaryResponse(
            String userId,
            String currency,
            double totalAssetsValue,
            double monthlyIncome,
            double monthlyExpenses,
            double monthlySavings,
            double savingsRate,
            int unpricedAssetsCount
    ) {
        public static SummaryResponse from(SummaryService.Summary s) {
            return new SummaryResponse(
                    s.userId().toString(),
                    s.currency(),
                    s.totalAssetsValue().doubleValue(),
                    s.monthlyIncome().doubleValue(),
                    s.monthlyExpenses().doubleValue(),
                    s.monthlySavings().doubleValue(),
                    s.savingsRate().doubleValue(),
                    s.unpricedAssetsCount()
            );
        }
    }
}
