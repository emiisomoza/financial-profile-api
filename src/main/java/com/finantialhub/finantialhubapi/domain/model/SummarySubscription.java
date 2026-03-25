package com.finantialhub.finantialhubapi.domain.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Transient;
import org.springframework.data.domain.Persistable;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.temporal.TemporalAdjusters;
import java.util.UUID;

@Table("summary_subscriptions")
public class SummarySubscription implements Persistable<UUID> {

    public enum Frequency { WEEKLY, MONTHLY }
    public enum Status { ACTIVE, CANCELLED }

    @Id
    private UUID id;

    @Column("user_id")
    private UUID userId;

    private Frequency frequency;

    private String currency;

    private Status status;

    @Column("next_send_at")
    private Instant nextSendAt;

    @Column("created_at")
    private Instant createdAt;

    @Transient
    private boolean isNew;

    protected SummarySubscription() {}

    private SummarySubscription(UUID userId, Frequency frequency, String currency) {
        this.id = UUID.randomUUID();
        this.userId = userId;
        this.frequency = frequency;
        this.currency = currency;
        this.status = Status.ACTIVE;
        this.nextSendAt = computeNextSendAt(frequency);
        this.createdAt = Instant.now();
        this.isNew = true;
    }

    public static SummarySubscription createNew(UUID userId, Frequency frequency, String currency) {
        return new SummarySubscription(userId, frequency, currency);
    }

    // Constructor used by Spring Data JDBC when loading from DB
    public SummarySubscription(UUID id, UUID userId, Frequency frequency, String currency,
                                Status status, Instant nextSendAt, Instant createdAt) {
        this.id = id;
        this.userId = userId;
        this.frequency = frequency;
        this.currency = currency;
        this.status = status;
        this.nextSendAt = nextSendAt;
        this.createdAt = createdAt;
        this.isNew = false;
    }

    public SummarySubscription withFrequency(Frequency newFrequency) {
        return new SummarySubscription(
                this.id, this.userId, newFrequency, this.currency,
                this.status, computeNextSendAt(newFrequency), this.createdAt
        );
    }

    public SummarySubscription cancelled() {
        return new SummarySubscription(
                this.id, this.userId, this.frequency, this.currency,
                Status.CANCELLED, this.nextSendAt, this.createdAt
        );
    }

    public SummarySubscription withNextSendAt(Instant next) {
        return new SummarySubscription(
                this.id, this.userId, this.frequency, this.currency,
                this.status, next, this.createdAt
        );
    }

    private static Instant computeNextSendAt(Frequency frequency) {
        LocalDate today = LocalDate.now(ZoneOffset.UTC);
        LocalDate next = switch (frequency) {
            case WEEKLY -> today.with(TemporalAdjusters.next(java.time.DayOfWeek.MONDAY));
            case MONTHLY -> today.with(TemporalAdjusters.firstDayOfNextMonth());
        };
        return next.atStartOfDay(ZoneOffset.UTC).toInstant();
    }

    @Override
    public UUID getId() { return id; }

    @Override
    public boolean isNew() { return isNew; }

    public UUID getUserId() { return userId; }
    public Frequency getFrequency() { return frequency; }
    public String getCurrency() { return currency; }
    public Status getStatus() { return status; }
    public Instant getNextSendAt() { return nextSendAt; }
    public Instant getCreatedAt() { return createdAt; }
}
