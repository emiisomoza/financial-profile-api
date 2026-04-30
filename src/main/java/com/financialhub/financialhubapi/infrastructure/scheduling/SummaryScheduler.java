package com.financialhub.financialhubapi.infrastructure.scheduling;

import com.financialhub.financialhubapi.application.usecases.SummaryService;
import com.financialhub.financialhubapi.domain.model.SummarySubscription;
import com.financialhub.financialhubapi.domain.model.User;
import com.financialhub.financialhubapi.domain.ports.SummaryPublisherPort;
import com.financialhub.financialhubapi.infrastructure.persistence.SummarySubscriptionRepository;
import com.financialhub.financialhubapi.infrastructure.persistence.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.temporal.TemporalAdjusters;
import java.util.List;

@Component
public class SummaryScheduler {

    private static final Logger log = LoggerFactory.getLogger(SummaryScheduler.class);

    private final SummarySubscriptionRepository subscriptionRepository;
    private final UserRepository userRepository;
    private final SummaryService summaryService;
    private final SummaryPublisherPort summaryPublisher;

    public SummaryScheduler(SummarySubscriptionRepository subscriptionRepository,
                            UserRepository userRepository,
                            SummaryService summaryService,
                            SummaryPublisherPort summaryPublisher) {
        this.subscriptionRepository = subscriptionRepository;
        this.userRepository = userRepository;
        this.summaryService = summaryService;
        this.summaryPublisher = summaryPublisher;
    }

    // Runs every day at 9:00 AM UTC
    @Scheduled(cron = "0 0 9 * * *")
    @Transactional
    public void processDueSubscriptions() {
        List<SummarySubscription> due = subscriptionRepository
                .findByStatusAndNextSendAtBefore(SummarySubscription.Status.ACTIVE, Instant.now());

        if (due.isEmpty()) return;

        log.info("Processing {} due summary subscription(s)", due.size());

        for (SummarySubscription subscription : due) {
            try {
                processSubscription(subscription);
            } catch (Exception e) {
                log.error("Failed to process subscription {} for user {}: {}",
                        subscription.getId(), subscription.getUserId(), e.getMessage(), e);
            }
        }
    }

    private void processSubscription(SummarySubscription subscription) {
        User user = userRepository.findById(subscription.getUserId()).orElse(null);
        if (user == null) {
            log.warn("User {} not found for subscription {}, cancelling",
                    subscription.getUserId(), subscription.getId());
            subscriptionRepository.save(subscription.cancelled());
            return;
        }

        SummaryService.Summary summary = summaryService.getSummary(user.getId(), subscription.getCurrency());
        summaryPublisher.publish(summary, user);

        Instant next = computeNextSendAt(subscription.getFrequency());
        subscriptionRepository.save(subscription.withNextSendAt(next));

        log.info("Summary published for user {} ({}), next send at {}",
                user.getId(), subscription.getFrequency(), next);
    }

    private Instant computeNextSendAt(SummarySubscription.Frequency frequency) {
        LocalDate today = LocalDate.now(ZoneOffset.UTC);
        LocalDate next = switch (frequency) {
            case WEEKLY -> today.with(TemporalAdjusters.next(java.time.DayOfWeek.MONDAY));
            case MONTHLY -> today.with(TemporalAdjusters.firstDayOfNextMonth());
        };
        return next.atStartOfDay(ZoneOffset.UTC).toInstant();
    }
}
