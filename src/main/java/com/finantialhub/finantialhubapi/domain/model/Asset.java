package com.finantialhub.finantialhubapi.domain.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Transient;
import org.springframework.data.domain.Persistable;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Table("assets")
public class Asset implements Persistable<UUID> {

    @Id
    private UUID id;

    @Column("user_id")
    private UUID userId;

    private AssetType type;

    private String name;

    private String symbol;

    private BigDecimal quantity;

    @Column("valuation_mode")
    private ValuationMode valuationMode;

    @Column("manual_unit_value")
    private BigDecimal manualUnitValue;

    private String currency;

    @Column("created_at")
    private Instant createdAt;

    @Transient
    private boolean isNew;

    protected Asset() {
        // for framework use only
    }

    // constructor for new assets (from application code)
    private Asset(UUID userId,
                  AssetType type,
                  String name,
                  String symbol,
                  BigDecimal quantity,
                  ValuationMode valuationMode,
                  BigDecimal manualUnitValue,
                  String currency) {
        this.id = UUID.randomUUID();
        this.userId = userId;
        this.type = type;
        this.name = name;
        this.symbol = symbol;
        this.quantity = quantity;
        this.valuationMode = valuationMode;
        this.manualUnitValue = manualUnitValue;
        this.currency = currency;
        this.createdAt = Instant.now();
        this.isNew = true;
    }

    public static Asset createNew(UUID userId,
                                  AssetType type,
                                  String name,
                                  String symbol,
                                  BigDecimal quantity,
                                  ValuationMode valuationMode,
                                  BigDecimal manualUnitValue,
                                  String currency) {
        return new Asset(userId, type, name, symbol, quantity, valuationMode, manualUnitValue, currency);
    }

    // constructor used by Spring Data JDBC when loading from DB
    public Asset(UUID id,
                 UUID userId,
                 AssetType type,
                 String name,
                 String symbol,
                 BigDecimal quantity,
                 ValuationMode valuationMode,
                 BigDecimal manualUnitValue,
                 String currency,
                 Instant createdAt) {
        this.id = id;
        this.userId = userId;
        this.type = type;
        this.name = name;
        this.symbol = symbol;
        this.quantity = quantity;
        this.valuationMode = valuationMode;
        this.manualUnitValue = manualUnitValue;
        this.currency = currency;
        this.createdAt = createdAt;
        this.isNew = false;
    }

    // Persistable implementation
    @Override
    public UUID getId() {
        return id;
    }

    @Override
    public boolean isNew() {
        return isNew;
    }

    // getters

    public UUID getUserId() {
        return userId;
    }

    public AssetType getType() {
        return type;
    }

    public String getName() {
        return name;
    }

    public String getSymbol() {
        return symbol;
    }

    public BigDecimal getQuantity() {
        return quantity;
    }

    public ValuationMode getValuationMode() {
        return valuationMode;
    }

    public BigDecimal getManualUnitValue() {
        return manualUnitValue;
    }

    public String getCurrency() {
        return currency;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
