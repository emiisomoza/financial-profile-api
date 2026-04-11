package com.finantialhub.finantialhubapi.infrastructure.web.dto;

import com.finantialhub.finantialhubapi.domain.model.SummarySubscription;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import org.springframework.lang.Nullable;

import java.util.UUID;

public class SummarySubscriptionDtos {

    public record CreateSubscriptionRequest(
            @Nullable UUID userId,
            @NotNull SummarySubscription.Frequency frequency,
            @NotBlank @Pattern(regexp = "^[A-Z]{3}$", message = "must be a 3-letter currency code") String currency
    ) {}

    public record UpdateSubscriptionRequest(
            @NotNull SummarySubscription.Frequency frequency
    ) {}

    public record SubscriptionResponse(
            String id,
            String userId,
            String frequency,
            String currency,
            String status,
            String nextSendAt,
            String createdAt
    ) {
        public static SubscriptionResponse from(SummarySubscription s) {
            return new SubscriptionResponse(
                    s.getId().toString(),
                    s.getUserId().toString(),
                    s.getFrequency().name(),
                    s.getCurrency(),
                    s.getStatus().name(),
                    s.getNextSendAt().toString(),
                    s.getCreatedAt().toString()
            );
        }
    }
}
