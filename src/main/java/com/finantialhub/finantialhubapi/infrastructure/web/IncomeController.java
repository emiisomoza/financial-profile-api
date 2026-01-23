package com.finantialhub.finantialhubapi.infrastructure.web;

import com.finantialhub.finantialhubapi.application.usecases.IncomeService;
import com.finantialhub.finantialhubapi.domain.model.Income;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.net.URI;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/incomes")
public class IncomeController {

    private final IncomeService incomeService;

    public IncomeController(IncomeService incomeService) {
        this.incomeService = incomeService;
    }

    @PostMapping
    public ResponseEntity<IncomeResponse> createIncome(@RequestBody CreateIncomeRequest request) {
        IncomeService.CreateIncomeCommand cmd = new IncomeService.CreateIncomeCommand(
                UUID.fromString(request.userId()),
                request.source(),
                request.frequency(),
                request.amount(),
                request.currency(),
                request.startsAt(),
                request.endsAt()
        );

        Income income = incomeService.createIncome(cmd);
        URI location = URI.create("/api/v1/incomes/" + income.getId());
        return ResponseEntity.created(location).body(IncomeResponse.from(income));
    }

    @GetMapping
    public List<IncomeResponse> listIncomes(@RequestParam("userId") UUID userId) {
        return incomeService.getIncomesForUser(userId)
                .stream()
                .map(IncomeResponse::from)
                .toList();
    }

    public record CreateIncomeRequest(
            String userId,
            String source,
            String frequency,
            BigDecimal amount,
            String currency,
            LocalDate startsAt,
            LocalDate endsAt
    ) {}

    public record IncomeResponse(
            String id,
            String userId,
            String source,
            String frequency,
            BigDecimal amount,
            String currency,
            LocalDate startsAt,
            LocalDate endsAt
    ) {
        public static IncomeResponse from(Income income) {
            return new IncomeResponse(
                    income.getId().toString(),
                    income.getUserId().toString(),
                    income.getSource(),
                    income.getFrequency().name(),
                    income.getAmount(),
                    income.getCurrency(),
                    income.getStartsAt(),
                    income.getEndsAt()
            );
        }
    }
}
