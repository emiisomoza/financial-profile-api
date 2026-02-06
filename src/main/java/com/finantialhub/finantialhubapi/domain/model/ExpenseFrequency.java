package com.finantialhub.finantialhubapi.domain.model;

public enum ExpenseFrequency {
    MONTHLY,
    WEEKLY,
    FORTNIGHTLY,
    YEARLY,
    ONE_TIME;

    public static ExpenseFrequency fromString(String value) {
        if (value == null) throw new IllegalArgumentException("frequency cannot be null");
        return ExpenseFrequency.valueOf(value.trim().toUpperCase());
    }

    public static String toString(ExpenseFrequency value) {
        if (value == null) throw new IllegalArgumentException("frequency cannot be null");
        return value.name();
    }
}
