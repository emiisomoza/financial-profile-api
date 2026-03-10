package com.finantialhub.finantialhubapi.infrastructure.web;

import com.finantialhub.finantialhubapi.application.usecases.SummaryService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.finantialhub.finantialhubapi.infrastructure.web.dto.SummaryDtos.SummaryResponse;

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
            @PathVariable UUID userId,
            @RequestParam(defaultValue = "AUD") String currency) {
        SummaryService.Summary summary = summaryService.getSummary(userId, currency);
        return ResponseEntity.ok(SummaryResponse.from(summary));
    }
}
