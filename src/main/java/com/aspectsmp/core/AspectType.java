package com.aspectsmp.core;

import java.util.Locale;

public enum AspectType {
    INFERNO("Inferno", "🔥"),
    TIDE("Tide", "🌊"),
    TEMPEST("Tempest", "⚡"),
    RIFT("Rift", "🌌"),
    VITALITY("Vitality", "🌿"),
    WAR("War", "⚔"),
    COSMOS("Cosmos", "🌠"),
    FORTUNE("Fortune", "💎");

    private final String displayName;
    private final String emoji;

    AspectType(String displayName, String emoji) {
        this.displayName = displayName;
        this.emoji = emoji;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getEmoji() {
        return emoji;
    }

    public String getConfigKey() {
        return name().toLowerCase(Locale.ROOT);
    }

    public static AspectType fromName(String name) {
        try {
            return valueOf(name.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            return INFERNO;
        }
    }
}