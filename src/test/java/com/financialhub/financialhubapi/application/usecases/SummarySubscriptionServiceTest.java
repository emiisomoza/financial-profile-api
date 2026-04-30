package com.financialhub.financialhubapi.application.usecases;

import com.financialhub.financialhubapi.domain.exceptions.SubscriptionNotFoundException;
import com.financialhub.financialhubapi.domain.exceptions.UserNotFoundException;
import com.financialhub.financialhubapi.domain.model.SummarySubscription;
import com.financialhub.financialhubapi.domain.model.SummarySubscription.Frequency;
import com.financialhub.financialhubapi.infrastructure.persistence.SummarySubscriptionRepository;
import com.financialhub.financialhubapi.infrastructure.persistence.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static com.financialhub.financialhubapi.domain.model.SummarySubscription.Frequency.MONTHLY;
import static com.financialhub.financialhubapi.domain.model.SummarySubscription.Frequency.WEEKLY;
import static com.financialhub.financialhubapi.domain.model.SummarySubscription.Status.ACTIVE;
import static com.financialhub.financialhubapi.domain.model.SummarySubscription.Status.CANCELLED;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SummarySubscriptionServiceTest {

    @Mock
    private SummarySubscriptionRepository subscriptionRepository;

    @Mock
    private UserRepository userRepository;

    private SummarySubscriptionService service;

    @BeforeEach
    void setUp() {
        service = new SummarySubscriptionService(subscriptionRepository, userRepository);
    }

    @Test
    void create_whenNoExistingActive_savesNewActiveSubscription() {
        UUID userId = UUID.randomUUID();
        when(userRepository.existsById(userId)).thenReturn(true);
        when(subscriptionRepository.findByUserIdAndStatus(userId, ACTIVE)).thenReturn(Optional.empty());
        when(subscriptionRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        SummarySubscription result = service.create(userId, WEEKLY, "AUD");

        assertThat(result.getUserId()).isEqualTo(userId);
        assertThat(result.getFrequency()).isEqualTo(WEEKLY);
        assertThat(result.getCurrency()).isEqualTo("AUD");
        assertThat(result.getStatus()).isEqualTo(ACTIVE);
        assertThat(result.getNextSendAt()).isAfter(Instant.now());
    }

    @Test
    void create_whenExistingActiveSubscription_cancelsPreviousAndCreatesNew() {
        UUID userId = UUID.randomUUID();
        SummarySubscription existing = activeSubscription(userId, MONTHLY, "AUD");

        when(userRepository.existsById(userId)).thenReturn(true);
        when(subscriptionRepository.findByUserIdAndStatus(userId, ACTIVE)).thenReturn(Optional.of(existing));
        when(subscriptionRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        service.create(userId, WEEKLY, "USD");

        ArgumentCaptor<SummarySubscription> captor = ArgumentCaptor.forClass(SummarySubscription.class);
        verify(subscriptionRepository, times(2)).save(captor.capture());

        List<SummarySubscription> saved = captor.getAllValues();
        assertThat(saved.get(0).getStatus()).isEqualTo(CANCELLED);
        assertThat(saved.get(1).getFrequency()).isEqualTo(WEEKLY);
        assertThat(saved.get(1).getStatus()).isEqualTo(ACTIVE);
    }

    @Test
    void create_whenUserNotFound_throwsUserNotFoundException() {
        UUID userId = UUID.randomUUID();
        when(userRepository.existsById(userId)).thenReturn(false);

        assertThrows(UserNotFoundException.class, () -> service.create(userId, WEEKLY, "AUD"));
        verifyNoInteractions(subscriptionRepository);
    }

    @Test
    void updateFrequency_changesFrequencyAndRecomputesNextSendAt() {
        UUID subId = UUID.randomUUID();
        SummarySubscription existing = activeSubscription(UUID.randomUUID(), WEEKLY, "AUD");
        when(subscriptionRepository.findById(subId)).thenReturn(Optional.of(existing));
        when(subscriptionRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        SummarySubscription result = service.updateFrequency(subId, MONTHLY);

        assertThat(result.getFrequency()).isEqualTo(MONTHLY);
        assertThat(result.getStatus()).isEqualTo(ACTIVE);
        assertThat(result.getNextSendAt()).isAfter(Instant.now());
    }

    @Test
    void updateFrequency_whenNotFound_throwsSubscriptionNotFoundException() {
        UUID subId = UUID.randomUUID();
        when(subscriptionRepository.findById(subId)).thenReturn(Optional.empty());

        assertThrows(SubscriptionNotFoundException.class, () -> service.updateFrequency(subId, MONTHLY));
    }

    @Test
    void updateFrequency_whenCancelled_throwsSubscriptionNotFoundException() {
        UUID subId = UUID.randomUUID();
        SummarySubscription cancelled = cancelledSubscription(UUID.randomUUID(), WEEKLY, "AUD");
        when(subscriptionRepository.findById(subId)).thenReturn(Optional.of(cancelled));

        assertThrows(SubscriptionNotFoundException.class, () -> service.updateFrequency(subId, MONTHLY));
    }

    @Test
    void cancel_setsStatusToCancelled() {
        UUID subId = UUID.randomUUID();
        SummarySubscription existing = activeSubscription(UUID.randomUUID(), WEEKLY, "AUD");
        when(subscriptionRepository.findById(subId)).thenReturn(Optional.of(existing));
        when(subscriptionRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        service.cancel(subId);

        ArgumentCaptor<SummarySubscription> captor = ArgumentCaptor.forClass(SummarySubscription.class);
        verify(subscriptionRepository).save(captor.capture());
        assertThat(captor.getValue().getStatus()).isEqualTo(CANCELLED);
    }

    @Test
    void cancel_whenNotFound_throwsSubscriptionNotFoundException() {
        UUID subId = UUID.randomUUID();
        when(subscriptionRepository.findById(subId)).thenReturn(Optional.empty());

        assertThrows(SubscriptionNotFoundException.class, () -> service.cancel(subId));
    }

    @Test
    void cancel_whenAlreadyCancelled_throwsSubscriptionNotFoundException() {
        UUID subId = UUID.randomUUID();
        SummarySubscription cancelled = cancelledSubscription(UUID.randomUUID(), WEEKLY, "AUD");
        when(subscriptionRepository.findById(subId)).thenReturn(Optional.of(cancelled));

        assertThrows(SubscriptionNotFoundException.class, () -> service.cancel(subId));
    }

    @Test
    void getActiveForUser_returnsActiveSubscription() {
        UUID userId = UUID.randomUUID();
        SummarySubscription active = activeSubscription(userId, WEEKLY, "AUD");
        when(subscriptionRepository.findByUserIdAndStatus(userId, ACTIVE)).thenReturn(Optional.of(active));

        SummarySubscription result = service.getActiveForUser(userId);

        assertThat(result.getUserId()).isEqualTo(userId);
        assertThat(result.getStatus()).isEqualTo(ACTIVE);
    }

    @Test
    void getActiveForUser_whenNoneActive_throwsSubscriptionNotFoundException() {
        UUID userId = UUID.randomUUID();
        when(subscriptionRepository.findByUserIdAndStatus(userId, ACTIVE)).thenReturn(Optional.empty());

        assertThrows(SubscriptionNotFoundException.class, () -> service.getActiveForUser(userId));
    }

    // ── helpers ──────────────────────────────────────────────────────────────

    private static SummarySubscription activeSubscription(UUID userId, Frequency frequency, String currency) {
        return new SummarySubscription(
                UUID.randomUUID(), userId, frequency, currency,
                ACTIVE, Instant.now().plusSeconds(3600), Instant.now()
        );
    }

    private static SummarySubscription cancelledSubscription(UUID userId, Frequency frequency, String currency) {
        return new SummarySubscription(
                UUID.randomUUID(), userId, frequency, currency,
                CANCELLED, Instant.now().minusSeconds(60), Instant.now()
        );
    }
}
