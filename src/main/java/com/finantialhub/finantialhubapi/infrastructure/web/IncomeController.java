package com.finantialhub.finantialhubapi.infrastructure.web;

import com.finantialhub.finantialhubapi.application.usecases.IncomeService;
import com.finantialhub.finantialhubapi.infrastructure.security.SecurityUtils;
import com.finantialhub.finantialhubapi.infrastructure.web.dto.IncomeDtos.*;
import com.finantialhub.finantialhubapi.domain.model.Income;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
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
    public ResponseEntity<IncomeResponse> createIncome(
            @AuthenticationPrincipal Jwt jwt,
            @RequestBody CreateIncomeRequest request) {

        UUID userId = SecurityUtils.resolveUserId(jwt,
                request.userId() != null ? UUID.fromString(request.userId()) : null);

        IncomeService.CreateIncomeCommand cmd = new IncomeService.CreateIncomeCommand(
                userId,
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
    public List<IncomeResponse> listIncomes(
            @AuthenticationPrincipal Jwt jwt,
            @RequestParam(required = false) UUID userId) {

        UUID resolvedId = SecurityUtils.resolveUserId(jwt, userId);
        return incomeService.getIncomesForUser(resolvedId)
                .stream()
                .map(IncomeResponse::from)
                .toList();
    }

    @PutMapping("/{id}")
    public ResponseEntity<IncomeResponse> updateIncome(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID id,
            @RequestBody UpdateIncomeRequest request) {

        Income existing = incomeService.getIncomeById(id);
        SecurityUtils.requireOwnership(jwt, existing.getUserId());

        IncomeService.UpdateIncomeCommand cmd = new IncomeService.UpdateIncomeCommand(
                request.source(),
                request.frequency(),
                request.amount(),
                request.currency(),
                request.startsAt(),
                request.endsAt()
        );

        Income updated = incomeService.updateIncome(id, cmd);
        return ResponseEntity.ok(IncomeResponse.from(updated));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteIncome(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID id) {

        Income existing = incomeService.getIncomeById(id);
        SecurityUtils.requireOwnership(jwt, existing.getUserId());

        incomeService.deleteIncome(id);
        return ResponseEntity.noContent().build();
    }
}
