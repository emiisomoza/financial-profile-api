package com.finantialhub.finantialhubapi.domain.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Transient;
import org.springframework.data.domain.Persistable;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Table("expenses")
public class Expense implements Persistable<UUID> {

    @Id
    private UUID id;

    @Column("user_id")
    private UUID userId;

    private ExpenseCategory category;

    private String description;

    private ExpenseFrequency frequency;

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

    protected Expense() {
        // for framework use only
    }

    // constructor for NEW expenses (used by createNew)
    private Expense(UUID userId,
                    ExpenseCategory category,
                    String description,
                    ExpenseFrequency frequency,
                    BigDecimal amount,
                    String currency,
                    LocalDate startsAt,
                    LocalDate endsAt) {
        this.id = UUID.randomUUID();
        this.userId = userId;
        this.category = category;
        this.description = description;
        this.frequency = frequency;
        this.amount = amount;
        this.currency = currency;
        this.startsAt = startsAt;
        this.endsAt = endsAt;
        this.createdAt = Instant.now();
        this.isNew = true;
    }

    public static Expense createNew(UUID userId,
                                    ExpenseCategory category,
                                    String description,
                                    ExpenseFrequency frequency,
                                    BigDecimal amount,
                                    String currency,
                                    LocalDate startsAt,
                                    LocalDate endsAt) {
        return new Expense(userId, category, description, frequency, amount, currency, startsAt, endsAt);
    }

    // ✅ public constructor used by Spring Data JDBC when reading from DB (and for tests)
    public Expense(UUID id,
                   UUID userId,
                   ExpenseCategory category,
                   String description,
                   ExpenseFrequency frequency,
                   BigDecimal amount,
                   String currency,
                   LocalDate startsAt,
                   LocalDate endsAt,
                   Instant createdAt) {
        this.id = id;
        this.userId = userId;
        this.category = category;
        this.description = description;
        this.frequency = frequency;
        this.amount = amount;
        this.currency = currency;
        this.startsAt = startsAt;
        this.endsAt = endsAt;
        this.createdAt = createdAt;
        this.isNew = false;
    }

    public Expense update(ExpenseCategory category,
                          String description,
                          ExpenseFrequency frequency,
                          BigDecimal amount,
                          String currency,
                          LocalDate startsAt,
                          LocalDate endsAt) {
        return new Expense(this.id, this.userId, category, description, frequency, amount, currency, startsAt, endsAt, this.createdAt);
    }

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
    public ExpenseCategory getCategory() { return category; }
    public String getDescription() { return description; }
    public ExpenseFrequency getFrequency() { return frequency; }
    public BigDecimal getAmount() { return amount; }
    public String getCurrency() { return currency; }
    public LocalDate getStartsAt() { return startsAt; }
    public LocalDate getEndsAt() { return endsAt; }
    public Instant getCreatedAt() { return createdAt; }
}
