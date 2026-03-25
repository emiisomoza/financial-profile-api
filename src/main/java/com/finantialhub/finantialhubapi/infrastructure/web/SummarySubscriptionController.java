package com.finantialhub.finantialhubapi.infrastructure.web;

import com.finantialhub.finantialhubapi.application.usecases.SummarySubscriptionService;
import com.finantialhub.finantialhubapi.domain.model.SummarySubscription;
import com.finantialhub.finantialhubapi.infrastructure.web.dto.SummarySubscriptionDtos.CreateSubscriptionRequest;
import com.finantialhub.finantialhubapi.infrastructure.web.dto.SummarySubscriptionDtos.SubscriptionResponse;
import com.finantialhub.finantialhubapi.infrastructure.web.dto.SummarySubscriptionDtos.UpdateSubscriptionRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/summary-subscriptions")
public class SummarySubscriptionController {

    private final SummarySubscriptionService subscriptionService;

    public SummarySubscriptionController(SummarySubscriptionService subscriptionService) {
        this.subscriptionService = subscriptionService;
    }

    @PostMapping
    public ResponseEntity<SubscriptionResponse> create(@Valid @RequestBody CreateSubscriptionRequest request) {
        SummarySubscription subscription = subscriptionService.create(
                request.userId(), request.frequency(), request.currency());
        return ResponseEntity.status(HttpStatus.CREATED).body(SubscriptionResponse.from(subscription));
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<SubscriptionResponse> getActiveForUser(@PathVariable UUID userId) {
        SummarySubscription subscription = subscriptionService.getActiveForUser(userId);
        return ResponseEntity.ok(SubscriptionResponse.from(subscription));
    }

    @PutMapping("/{id}")
    public ResponseEntity<SubscriptionResponse> updateFrequency(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateSubscriptionRequest request) {
        SummarySubscription subscription = subscriptionService.updateFrequency(id, request.frequency());
        return ResponseEntity.ok(SubscriptionResponse.from(subscription));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> cancel(@PathVariable UUID id) {
        subscriptionService.cancel(id);
        return ResponseEntity.noContent().build();
    }
}
