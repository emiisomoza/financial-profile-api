package com.financialhub.financialhubapi.infrastructure.web;

import com.financialhub.financialhubapi.application.usecases.SummaryService;
import com.financialhub.financialhubapi.infrastructure.security.SecurityUtils;
import com.financialhub.financialhubapi.infrastructure.web.dto.SummaryDtos.SummaryResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/summary")
public class SummaryController {

    private final SummaryService summaryService;

    public SummaryController(SummaryService summaryService) {
        this.summaryService = summaryService;
    }

    @GetMapping("/{userId}")
    public ResponseEntity<SummaryResponse> getSummary(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID userId,
            @RequestParam(defaultValue = "AUD") String currency) {

        SecurityUtils.requireOwnership(jwt, userId);

        SummaryService.Summary summary = summaryService.getSummary(userId, currency);
        return ResponseEntity.ok(SummaryResponse.from(summary));
    }
}
