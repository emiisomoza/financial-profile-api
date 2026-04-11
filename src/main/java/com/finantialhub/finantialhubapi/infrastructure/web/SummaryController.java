package com.finantialhub.finantialhubapi.infrastructure.web;

import com.finantialhub.finantialhubapi.application.usecases.SummaryService;
import com.finantialhub.finantialhubapi.infrastructure.security.SecurityUtils;
import com.finantialhub.finantialhubapi.infrastructure.web.dto.SummaryDtos.SummaryResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

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

        if (!SecurityUtils.isAdmin(jwt) && !userId.equals(SecurityUtils.extractUserId(jwt))) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }

        SummaryService.Summary summary = summaryService.getSummary(userId, currency);
        return ResponseEntity.ok(SummaryResponse.from(summary));
    }
}
