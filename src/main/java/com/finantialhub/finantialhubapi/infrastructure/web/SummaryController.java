package com.finantialhub.finantialhubapi.infrastructure.web;

import com.finantialhub.finantialhubapi.application.usecases.SummaryService;
import org.springframework.web.bind.annotation.*;
import com.finantialhub.finantialhubapi.infrastructure.web.dto.SummaryDtos.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/summary")
public class SummaryController {

    private final SummaryService summaryService;

    public SummaryController(SummaryService summaryService) {
        this.summaryService = summaryService;
    }

    @GetMapping
    public SummaryResponse getSummary(@RequestParam("userId") UUID userId) {
        return SummaryResponse.from(summaryService.getSummary(userId));
    }
}
