package com.aspectsmp.core;

import org.bukkit.configuration.ConfigurationSection;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class Heart {

    private final UUID owner;
    private AspectType aspect;
    private int tier;
    private int stability;
    private long essence;
    private final Map<String, Long> cooldowns;
    private boolean dormant;
    private boolean cloudUnlocked;
    private boolean winterUnlocked;

    public Heart(UUID owner, AspectType aspect) {
        this.owner = owner;
        this.aspect = aspect;
        this.tier = 1;
        this.stability = 100;
        this.essence = 0;
        this.cooldowns = new HashMap<>();
        this.dormant = false;
        this.cloudUnlocked = false;
        this.winterUnlocked = false;
    }

    public Heart(UUID owner, AspectType aspect, int tier, int stability, long essence, boolean dormant) {
        this.owner = owner;
        this.aspect = aspect;
        this.tier = tier;
        this.stability = stability;
        this.essence = essence;
        this.cooldowns = new HashMap<>();
        this.dormant = dormant;
        this.cloudUnlocked = false;
        this.winterUnlocked = false;
    }

    public UUID getOwner() {
        return owner;
    }

    public AspectType getAspect() {
        return aspect;
    }

    public void setAspect(AspectType aspect) {
        this.aspect = aspect;
    }

    public int getTier() {
        return tier;
    }

    public void setTier(int tier) {
        this.tier = Math.max(1, Math.min(3, tier));
    }

    public int getStability() {
        return stability;
    }

    public void setStability(int stability) {
        this.stability = Math.max(0, Math.min(100, stability));
        if (this.stability == 0) {
            this.dormant = true;
        }
    }

    public void addStability(int amount) {
        setStability(this.stability + amount);
    }

    public long getEssence() {
        return essence;
    }

    public void setEssence(long essence) {
        this.essence = Math.max(0, essence);
    }

    public void addEssence(long amount) {
        this.essence = Math.max(0, this.essence + amount);
    }

    public Map<String, Long> getCooldowns() {
        return cooldowns;
    }

    public boolean isDormant() {
        return dormant;
    }

    public void setDormant(boolean dormant) {
        this.dormant = dormant;
        if (!dormant) {
            this.stability = Math.max(1, this.stability);
        }
    }

    public boolean isCloudUnlocked() {
        return cloudUnlocked;
    }

    public void setCloudUnlocked(boolean cloudUnlocked) {
        this.cloudUnlocked = cloudUnlocked;
    }

    public boolean isWinterUnlocked() {
        return winterUnlocked;
    }

    public void setWinterUnlocked(boolean winterUnlocked) {
        this.winterUnlocked = winterUnlocked;
    }

    public boolean canUseAbilities() {
        return !dormant && tier > 0;
    }

    public StabilityState getStabilityState() {
        if (stability >= 90) return StabilityState.AWAKENED;
        if (stability >= 70) return StabilityState.STABLE;
        if (stability >= 50) return StabilityState.FRACTURED;
        if (stability >= 30) return StabilityState.FADED;
        if (stability >= 1) return StabilityState.CRITICAL;
        return StabilityState.DORMANT;
    }

    public int getPassiveLevel() {
        if (stability >= 90) return 2;
        if (stability >= 50) return 1;
        return 0;
    }

    public void save(ConfigurationSection section) {
        section.set("aspect", aspect.name());
        section.set("tier", tier);
        section.set("stability", stability);
        section.set("essence", essence);
        section.set("dormant", dormant);
        section.set("cloud_unlocked", cloudUnlocked);
        section.set("winter_unlocked", winterUnlocked);
        section.set("cooldowns", cooldowns);
    }

    public static Heart load(ConfigurationSection section) {
        String aspectName = section.getString("aspect", "INFERNO");
        AspectType aspect = AspectType.fromName(aspectName);
        int tier = section.getInt("tier", 1);
        int stability = section.getInt("stability", 100);
        long essence = section.getLong("essence", 0);
        boolean dormant = section.getBoolean("dormant", false);
        boolean cloudUnlocked = section.getBoolean("cloud_unlocked", false);
        boolean winterUnlocked = section.getBoolean("winter_unlocked", false);
        
        Heart heart = new Heart(UUID.fromString(section.getName()), aspect, tier, stability, essence, dormant);
        heart.setCloudUnlocked(cloudUnlocked);
        heart.setWinterUnlocked(winterUnlocked);
        heart.getCooldowns().putAll(section.getConfigurationSection("cooldowns") != null ? 
            section.getConfigurationSection("cooldowns").getValues(false).entrySet().stream()
                .collect(java.util.stream.Collectors.toMap(
                    e -> e.getKey(),
                    e -> ((Number) e.getValue()).longValue())) : 
            new java.util.HashMap<>());
        return heart;
    }
}