package com.aspectsmp.core;

public enum StabilityState {
    DORMANT("Dormant", 0),
    CRITICAL("Critical", 29),
    FADED("Faded", 49),
    FRACTURED("Fractured", 69),
    STABLE("Stable", 89),
    AWAKENED("Awakened", 100);

    private final String displayName;
    private final int maxStability;

    StabilityState(String displayName, int maxStability) {
        this.displayName = displayName;
        this.maxStability = maxStability;
    }

    public String getDisplayName() {
        return displayName;
    }

    public int getMaxStability() {
        return maxStability;
    }
}