package com.financialhub.financialhubapi.infrastructure.persistence;

import com.financialhub.financialhubapi.domain.model.SummarySubscription;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jdbc.test.autoconfigure.DataJdbcTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.jdbc.core.JdbcTemplate;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static com.financialhub.financialhubapi.domain.model.SummarySubscription.Frequency.MONTHLY;
import static com.financialhub.financialhubapi.domain.model.SummarySubscription.Frequency.WEEKLY;
import static com.financialhub.financialhubapi.domain.model.SummarySubscription.Status.ACTIVE;
import static org.assertj.core.api.Assertions.assertThat;

@DataJdbcTest
@Testcontainers
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class SummarySubscriptionRepositoryTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:latest");

    @Autowired
    SummarySubscriptionRepository subscriptionRepository;

    @Autowired
    JdbcTemplate jdbcTemplate;

    private UUID userId;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        jdbcTemplate.update(
                "INSERT INTO users (id, email, full_name, password_hash, created_at, role) VALUES (?, ?, ?, ?, ?, ?)",
                userId, "test@example.com", "Test User", "hash", Timestamp.from(Instant.now()), "MEMBER"
        );
    }

    @AfterEach
    void tearDown() {
        jdbcTemplate.update("DELETE FROM summary_subscriptions");
        jdbcTemplate.update("DELETE FROM users");
    }

    // ── findByUserIdAndStatus ─────────────────────────────────────────────────

    @Test
    void findByUserIdAndStatus_whenActiveExists_returnsIt() {
        SummarySubscription saved = subscriptionRepository.save(SummarySubscription.createNew(userId, WEEKLY, "AUD"));

        Optional<SummarySubscription> result = subscriptionRepository
                .findByUserIdAndStatus(userId, ACTIVE);

        assertThat(result).isPresent();
        assertThat(result.get().getId()).isEqualTo(saved.getId());
        assertThat(result.get().getFrequency()).isEqualTo(WEEKLY);
        assertThat(result.get().getCurrency()).isEqualTo("AUD");
        assertThat(result.get().getStatus()).isEqualTo(ACTIVE);
    }

    @Test
    void findByUserIdAndStatus_whenCancelled_returnsEmpty() {
        SummarySubscription saved = subscriptionRepository.save(SummarySubscription.createNew(userId, WEEKLY, "AUD"));
        subscriptionRepository.save(saved.cancelled());

        Optional<SummarySubscription> result = subscriptionRepository
                .findByUserIdAndStatus(userId, ACTIVE);

        assertThat(result).isEmpty();
    }

    @Test
    void findByUserIdAndStatus_whenNoSubscriptionExists_returnsEmpty() {
        Optional<SummarySubscription> result = subscriptionRepository
                .findByUserIdAndStatus(userId, ACTIVE);

        assertThat(result).isEmpty();
    }

    // ── findByStatusAndNextSendAtBefore ───────────────────────────────────────

    @Test
    void findByStatusAndNextSendAtBefore_returnsDueSubscriptions() {
        // Due: save with createNew (future nextSendAt), then move it to the past
        SummarySubscription due = subscriptionRepository.save(SummarySubscription.createNew(userId, WEEKLY, "AUD"));
        subscriptionRepository.save(due.withNextSendAt(Instant.now().minusSeconds(3600)));

        List<SummarySubscription> result = subscriptionRepository
                .findByStatusAndNextSendAtBefore(ACTIVE, Instant.now());

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getId()).isEqualTo(due.getId());
    }

    @Test
    void findByStatusAndNextSendAtBefore_doesNotReturnFutureSubscriptions() {
        // nextSendAt computed by createNew is always in the future
        subscriptionRepository.save(SummarySubscription.createNew(userId, WEEKLY, "AUD"));

        List<SummarySubscription> result = subscriptionRepository
                .findByStatusAndNextSendAtBefore(ACTIVE, Instant.now());

        assertThat(result).isEmpty();
    }

    @Test
    void findByStatusAndNextSendAtBefore_excludesCancelledSubscriptions() {
        SummarySubscription saved = subscriptionRepository.save(SummarySubscription.createNew(userId, WEEKLY, "AUD"));
        SummarySubscription cancelled = subscriptionRepository.save(saved.cancelled());
        // Set nextSendAt to past on the cancelled one
        subscriptionRepository.save(cancelled.withNextSendAt(Instant.now().minusSeconds(3600)));

        List<SummarySubscription> result = subscriptionRepository
                .findByStatusAndNextSendAtBefore(ACTIVE, Instant.now());

        assertThat(result).isEmpty();
    }

    @Test
    void findByStatusAndNextSendAtBefore_returnsOnlyDueAmongMixed() {
        // Due subscription
        SummarySubscription due = subscriptionRepository.save(SummarySubscription.createNew(userId, WEEKLY, "AUD"));
        subscriptionRepository.save(due.withNextSendAt(Instant.now().minusSeconds(3600)));

        // Not yet due (second user needed to avoid FK issue — reuse same userId, no unique constraint)
        UUID userId2 = UUID.randomUUID();
        jdbcTemplate.update(
                "INSERT INTO users (id, email, full_name, password_hash, created_at, role) VALUES (?, ?, ?, ?, ?, ?)",
                userId2, "other@example.com", "Other User", "hash", Timestamp.from(Instant.now()), "MEMBER"
        );
        subscriptionRepository.save(SummarySubscription.createNew(userId2, MONTHLY, "USD"));

        List<SummarySubscription> result = subscriptionRepository
                .findByStatusAndNextSendAtBefore(ACTIVE, Instant.now());

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getId()).isEqualTo(due.getId());
    }
}
