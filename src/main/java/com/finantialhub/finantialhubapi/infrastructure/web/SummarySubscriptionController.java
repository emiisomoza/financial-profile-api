package com.finantialhub.finantialhubapi.infrastructure.web;

import com.finantialhub.finantialhubapi.application.usecases.SummarySubscriptionService;
import com.finantialhub.finantialhubapi.domain.model.SummarySubscription;
import com.finantialhub.finantialhubapi.infrastructure.security.SecurityUtils;
import com.finantialhub.finantialhubapi.infrastructure.web.dto.SummarySubscriptionDtos.CreateSubscriptionRequest;
import com.finantialhub.finantialhubapi.infrastructure.web.dto.SummarySubscriptionDtos.SubscriptionResponse;
import com.finantialhub.finantialhubapi.infrastructure.web.dto.SummarySubscriptionDtos.UpdateSubscriptionRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/summary-subscriptions")
public class SummarySubscriptionController {

    private final SummarySubscriptionService subscriptionService;

    public SummarySubscriptionController(SummarySubscriptionService subscriptionService) {
        this.subscriptionService = subscriptionService;
    }

    @PostMapping
    public ResponseEntity<SubscriptionResponse> create(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody CreateSubscriptionRequest request) {

        UUID userId = SecurityUtils.resolveUserId(jwt, request.userId());
        SummarySubscription subscription = subscriptionService.create(userId, request.frequency(), request.currency());
        return ResponseEntity.status(HttpStatus.CREATED).body(SubscriptionResponse.from(subscription));
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<SubscriptionResponse> getActiveForUser(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID userId) {

        if (!SecurityUtils.isAdmin(jwt) && !userId.equals(SecurityUtils.extractUserId(jwt))) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }

        SummarySubscription subscription = subscriptionService.getActiveForUser(userId);
        return ResponseEntity.ok(SubscriptionResponse.from(subscription));
    }

    @PutMapping("/{id}")
    public ResponseEntity<SubscriptionResponse> updateFrequency(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID id,
            @Valid @RequestBody UpdateSubscriptionRequest request) {

        SummarySubscription existing = subscriptionService.getSubscriptionById(id);
        if (!SecurityUtils.isAdmin(jwt) && !existing.getUserId().equals(SecurityUtils.extractUserId(jwt))) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }

        SummarySubscription subscription = subscriptionService.updateFrequency(id, request.frequency());
        return ResponseEntity.ok(SubscriptionResponse.from(subscription));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> cancel(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID id) {

        SummarySubscription existing = subscriptionService.getSubscriptionById(id);
        if (!SecurityUtils.isAdmin(jwt) && !existing.getUserId().equals(SecurityUtils.extractUserId(jwt))) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }

        subscriptionService.cancel(id);
        return ResponseEntity.noContent().build();
    }
}
