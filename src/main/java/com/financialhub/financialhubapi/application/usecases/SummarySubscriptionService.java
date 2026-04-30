package com.financialhub.financialhubapi.application.usecases;

import com.financialhub.financialhubapi.domain.exceptions.SubscriptionNotFoundException;
import com.financialhub.financialhubapi.domain.exceptions.UserNotFoundException;
import com.financialhub.financialhubapi.domain.model.SummarySubscription;
import com.financialhub.financialhubapi.infrastructure.persistence.SummarySubscriptionRepository;
import com.financialhub.financialhubapi.infrastructure.persistence.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class SummarySubscriptionService {

    private final SummarySubscriptionRepository subscriptionRepository;
    private final UserRepository userRepository;

    public SummarySubscriptionService(SummarySubscriptionRepository subscriptionRepository,
                                      UserRepository userRepository) {
        this.subscriptionRepository = subscriptionRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public SummarySubscription create(UUID userId, SummarySubscription.Frequency frequency, String currency) {
        if (!userRepository.existsById(userId)) {
            throw new UserNotFoundException("User not found with id: " + userId);
        }

        // Cancel any existing active subscription before creating a new one
        subscriptionRepository.findByUserIdAndStatus(userId, SummarySubscription.Status.ACTIVE)
                .ifPresent(existing -> subscriptionRepository.save(existing.cancelled()));

        SummarySubscription subscription = SummarySubscription.createNew(userId, frequency, currency);
        return subscriptionRepository.save(subscription);
    }

    @Transactional
    public SummarySubscription updateFrequency(UUID subscriptionId, SummarySubscription.Frequency newFrequency) {
        SummarySubscription existing = getActiveSubscription(subscriptionId);
        return subscriptionRepository.save(existing.withFrequency(newFrequency));
    }

    @Transactional
    public void cancel(UUID subscriptionId) {
        SummarySubscription existing = getActiveSubscription(subscriptionId);
        subscriptionRepository.save(existing.cancelled());
    }

    public SummarySubscription getSubscriptionById(UUID subscriptionId) {
        return subscriptionRepository.findById(subscriptionId)
                .orElseThrow(() -> new SubscriptionNotFoundException(
                        "Subscription not found with id: " + subscriptionId));
    }

    public SummarySubscription getActiveForUser(UUID userId) {
        return subscriptionRepository.findByUserIdAndStatus(userId, SummarySubscription.Status.ACTIVE)
                .orElseThrow(() -> new SubscriptionNotFoundException(
                        "No active subscription found for user: " + userId));
    }

    private SummarySubscription getActiveSubscription(UUID subscriptionId) {
        SummarySubscription subscription = subscriptionRepository.findById(subscriptionId)
                .orElseThrow(() -> new SubscriptionNotFoundException(
                        "Subscription not found with id: " + subscriptionId));

        if (subscription.getStatus() == SummarySubscription.Status.CANCELLED) {
            throw new SubscriptionNotFoundException("Subscription " + subscriptionId + " is already cancelled");
        }
        return subscription;
    }
}
