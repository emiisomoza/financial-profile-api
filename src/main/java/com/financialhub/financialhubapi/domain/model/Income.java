package com.financialhub.financialhubapi.domain.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Transient;
import org.springframework.data.domain.Persistable;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Table("incomes")
public class Income implements Persistable<UUID> {

    @Id
    private UUID id;

    @Column("user_id")
    private UUID userId;

    private String source;

    private IncomeFrequency frequency;

    private BigDecimal amount;

    private String currency;

    @Column("starts_at")
    private LocalDate startsAt;

    @Column("ends_at")
    private LocalDate endsAt;

    @Column("created_at")
    private Instant createdAt;

    @Transient
    private boolean isNew;

    protected Income() {
        // for framework use only
    }

    // constructor for NEW incomes (from application code)
    private Income(UUID userId,
                   String source,
                   IncomeFrequency frequency,
                   BigDecimal amount,
                   String currency,
                   LocalDate startsAt,
                   LocalDate endsAt) {
        this.id = UUID.randomUUID();
        this.userId = userId;
        this.source = source;
        this.frequency = frequency;
        this.amount = amount;
        this.currency = currency;
        this.startsAt = startsAt;
        this.endsAt = endsAt;
        this.createdAt = Instant.now();
        this.isNew = true;
    }

    public static Income createNew(UUID userId,
                                   String source,
                                   IncomeFrequency frequency,
                                   BigDecimal amount,
                                   String currency,
                                   LocalDate startsAt,
                                   LocalDate endsAt) {
        return new Income(userId, source, frequency, amount, currency, startsAt, endsAt);
    }

    // constructor used by Spring Data JDBC when loading from DB (and great for tests)
    public Income(UUID id,
                  UUID userId,
                  String source,
                  IncomeFrequency frequency,
                  BigDecimal amount,
                  String currency,
                  LocalDate startsAt,
                  LocalDate endsAt,
                  Instant createdAt) {
        this.id = id;
        this.userId = userId;
        this.source = source;
        this.frequency = frequency;
        this.amount = amount;
        this.currency = currency;
        this.startsAt = startsAt;
        this.endsAt = endsAt;
        this.createdAt = createdAt;
        this.isNew = false;
    }

    public Income update(String source,
                         IncomeFrequency frequency,
                         BigDecimal amount,
                         String currency,
                         LocalDate startsAt,
                         LocalDate endsAt) {
        return new Income(this.id, this.userId, source, frequency, amount, currency, startsAt, endsAt, this.createdAt);
    }

    // Persistable
    @Override
    public UUID getId() {
        return id;
    }

    @Override
    public boolean isNew() {
        return isNew;
    }

    // getters
    public UUID getUserId() { return userId; }
    public String getSource() { return source; }
    public IncomeFrequency getFrequency() { return frequency; }
    public BigDecimal getAmount() { return amount; }
    public String getCurrency() { return currency; }
    public LocalDate getStartsAt() { return startsAt; }
    public LocalDate getEndsAt() { return endsAt; }
    public Instant getCreatedAt() { return createdAt; }
}
