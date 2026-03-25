package com.finantialhub.finantialhubapi.infrastructure.scheduling;

import com.finantialhub.finantialhubapi.application.usecases.SummaryService;
import com.finantialhub.finantialhubapi.domain.model.Role;
import com.finantialhub.finantialhubapi.domain.model.SummarySubscription;
import com.finantialhub.finantialhubapi.domain.ports.SummaryPublisherPort;
import com.finantialhub.finantialhubapi.infrastructure.persistence.SummarySubscriptionRepository;
import com.finantialhub.finantialhubapi.infrastructure.persistence.UserRepository;
import com.finantialhub.finantialhubapi.domain.model.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static com.finantialhub.finantialhubapi.domain.model.SummarySubscription.Frequency.MONTHLY;
import static com.finantialhub.finantialhubapi.domain.model.SummarySubscription.Frequency.WEEKLY;
import static com.finantialhub.finantialhubapi.domain.model.SummarySubscription.Status.ACTIVE;
import static com.finantialhub.finantialhubapi.domain.model.SummarySubscription.Status.CANCELLED;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SummarySchedulerTest {

    @Mock private SummarySubscriptionRepository subscriptionRepository;
    @Mock private UserRepository userRepository;
    @Mock private SummaryService summaryService;
    @Mock private SummaryPublisherPort summaryPublisher;

    private SummaryScheduler scheduler;

    @BeforeEach
    void setUp() {
        scheduler = new SummaryScheduler(subscriptionRepository, userRepository, summaryService, summaryPublisher);
    }

    @Test
    void processDueSubscriptions_whenNoDue_doesNothing() {
        when(subscriptionRepository.findByStatusAndNextSendAtBefore(eq(ACTIVE), any()))
                .thenReturn(List.of());

        scheduler.processDueSubscriptions();

        verifyNoInteractions(summaryService, summaryPublisher, userRepository);
    }

    @Test
    void processDueSubscriptions_publishesSummaryAndAdvancesNextSendAt() {
        UUID userId = UUID.randomUUID();
        User user = user(userId);
        SummarySubscription due = dueSubscription(userId, WEEKLY);
        SummaryService.Summary summary = emptySummary(userId);

        when(subscriptionRepository.findByStatusAndNextSendAtBefore(eq(ACTIVE), any()))
                .thenReturn(List.of(due));
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(summaryService.getSummary(userId, "AUD")).thenReturn(summary);
        when(subscriptionRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        scheduler.processDueSubscriptions();

        verify(summaryPublisher).publish(summary, user);

        ArgumentCaptor<SummarySubscription> captor = ArgumentCaptor.forClass(SummarySubscription.class);
        verify(subscriptionRepository).save(captor.capture());
        assertThat(captor.getValue().getNextSendAt()).isAfter(Instant.now());
    }

    @Test
    void processDueSubscriptions_weeklySubscription_nextSendAtIsNextMonday() {
        UUID userId = UUID.randomUUID();
        User user = user(userId);
        SummarySubscription due = dueSubscription(userId, WEEKLY);
        SummaryService.Summary summary = emptySummary(userId);

        when(subscriptionRepository.findByStatusAndNextSendAtBefore(eq(ACTIVE), any()))
                .thenReturn(List.of(due));
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(summaryService.getSummary(userId, "AUD")).thenReturn(summary);
        when(subscriptionRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        scheduler.processDueSubscriptions();

        ArgumentCaptor<SummarySubscription> captor = ArgumentCaptor.forClass(SummarySubscription.class);
        verify(subscriptionRepository).save(captor.capture());

        java.time.DayOfWeek dayOfWeek = captor.getValue().getNextSendAt()
                .atZone(java.time.ZoneOffset.UTC).getDayOfWeek();
        assertThat(dayOfWeek).isEqualTo(java.time.DayOfWeek.MONDAY);
    }

    @Test
    void processDueSubscriptions_monthlySubscription_nextSendAtIsFirstOfNextMonth() {
        UUID userId = UUID.randomUUID();
        User user = user(userId);
        SummarySubscription due = dueSubscription(userId, MONTHLY);
        SummaryService.Summary summary = emptySummary(userId);

        when(subscriptionRepository.findByStatusAndNextSendAtBefore(eq(ACTIVE), any()))
                .thenReturn(List.of(due));
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(summaryService.getSummary(userId, "AUD")).thenReturn(summary);
        when(subscriptionRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        scheduler.processDueSubscriptions();

        ArgumentCaptor<SummarySubscription> captor = ArgumentCaptor.forClass(SummarySubscription.class);
        verify(subscriptionRepository).save(captor.capture());

        int dayOfMonth = captor.getValue().getNextSendAt()
                .atZone(java.time.ZoneOffset.UTC).getDayOfMonth();
        assertThat(dayOfMonth).isEqualTo(1);
    }

    @Test
    void processDueSubscriptions_whenUserNotFound_cancelsSubscriptionAndSkipsPublish() {
        UUID userId = UUID.randomUUID();
        SummarySubscription due = dueSubscription(userId, WEEKLY);

        when(subscriptionRepository.findByStatusAndNextSendAtBefore(eq(ACTIVE), any()))
                .thenReturn(List.of(due));
        when(userRepository.findById(userId)).thenReturn(Optional.empty());
        when(subscriptionRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        scheduler.processDueSubscriptions();

        ArgumentCaptor<SummarySubscription> captor = ArgumentCaptor.forClass(SummarySubscription.class);
        verify(subscriptionRepository).save(captor.capture());
        assertThat(captor.getValue().getStatus()).isEqualTo(CANCELLED);
        verifyNoInteractions(summaryPublisher);
    }

    @Test
    void processDueSubscriptions_whenOneFailsPublish_continuesWithRemaining() {
        UUID userId1 = UUID.randomUUID();
        UUID userId2 = UUID.randomUUID();
        User user1 = user(userId1);
        User user2 = user(userId2);
        SummarySubscription due1 = dueSubscription(userId1, WEEKLY);
        SummarySubscription due2 = dueSubscription(userId2, WEEKLY);
        SummaryService.Summary summary1 = emptySummary(userId1);
        SummaryService.Summary summary2 = emptySummary(userId2);

        when(subscriptionRepository.findByStatusAndNextSendAtBefore(eq(ACTIVE), any()))
                .thenReturn(List.of(due1, due2));
        when(userRepository.findById(userId1)).thenReturn(Optional.of(user1));
        when(userRepository.findById(userId2)).thenReturn(Optional.of(user2));
        when(summaryService.getSummary(userId1, "AUD")).thenReturn(summary1);
        when(summaryService.getSummary(userId2, "AUD")).thenReturn(summary2);
        doThrow(new RuntimeException("Broker down")).when(summaryPublisher).publish(eq(summary1), any());
        when(subscriptionRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        scheduler.processDueSubscriptions();

        // Second subscription was still processed despite first failing
        verify(summaryPublisher).publish(summary2, user2);
    }

    // ── helpers ──────────────────────────────────────────────────────────────

    private static SummarySubscription dueSubscription(UUID userId, SummarySubscription.Frequency frequency) {
        return new SummarySubscription(
                UUID.randomUUID(), userId, frequency, "AUD",
                ACTIVE, Instant.now().minusSeconds(60), Instant.now()
        );
    }

    private static User user(UUID userId) {
        return new User(userId, "user@example.com", "Test User", "hash", Instant.now(), Role.MEMBER);
    }

    private static SummaryService.Summary emptySummary(UUID userId) {
        return new SummaryService.Summary(
                userId, "AUD",
                BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, 0
        );
    }
}
