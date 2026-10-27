package com.aspectsmp.core;

import org.bukkit.configuration.ConfigurationSection;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

public class Heart {

    private final UUID owner;
    private final AtomicInteger tier;
    private final AtomicInteger stability;
    private final AtomicLong essence;
    private final Map<String, Long> cooldowns;
    private final AtomicBoolean dormant;
    private final AtomicBoolean cloudUnlocked;
    private final AtomicBoolean winterUnlocked;
    private final AtomicBoolean shadowUnlocked;
    private final AtomicBoolean stormUnlocked;
    private volatile AspectType aspect;

    public Heart(UUID owner, AspectType aspect) {
        this.owner = owner;
        this.aspect = aspect;
        this.tier = new AtomicInteger(1);
        this.stability = new AtomicInteger(100);
        this.essence = new AtomicLong(0);
        this.cooldowns = new ConcurrentHashMap<>();
        this.dormant = new AtomicBoolean(false);
        this.cloudUnlocked = new AtomicBoolean(false);
        this.winterUnlocked = new AtomicBoolean(false);
        this.shadowUnlocked = new AtomicBoolean(false);
        this.stormUnlocked = new AtomicBoolean(false);
    }

    public Heart(UUID owner, AspectType aspect, int tier, int stability, long essence, boolean dormant) {
        this.owner = owner;
        this.aspect = aspect;
        this.tier = new AtomicInteger(Math.max(1, Math.min(3, tier)));
        this.stability = new AtomicInteger(Math.max(0, Math.min(100, stability)));
        this.essence = new AtomicLong(Math.max(0, essence));
        this.cooldowns = new ConcurrentHashMap<>();
        this.dormant = new AtomicBoolean(dormant);
        this.cloudUnlocked = new AtomicBoolean(false);
        this.winterUnlocked = new AtomicBoolean(false);
        this.shadowUnlocked = new AtomicBoolean(false);
        this.stormUnlocked = new AtomicBoolean(false);
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
        return tier.get();
    }

    public void setTier(int tier) {
        this.tier.set(Math.max(1, Math.min(3, tier)));
    }

    public int getStability() {
        return stability.get();
    }

    public void setStability(int stability) {
        int clamped = Math.max(0, Math.min(100, stability));
        this.stability.set(clamped);
        if (clamped == 0) {
            this.dormant.set(true);
        }
    }

    public void addStability(int amount) {
        int newVal;
        int current;
        do {
            current = this.stability.get();
            newVal = Math.max(0, Math.min(100, current + amount));
        } while (!this.stability.compareAndSet(current, newVal));
        if (newVal == 0) {
            this.dormant.set(true);
        }
    }

    public long getEssence() {
        return essence.get();
    }

    public void setEssence(long essence) {
        this.essence.set(Math.max(0, essence));
    }

    public void addEssence(long amount) {
        long newVal;
        long current;
        do {
            current = this.essence.get();
            newVal = Math.max(0, current + amount);
        } while (!this.essence.compareAndSet(current, newVal));
    }

    public Map<String, Long> getCooldowns() {
        return cooldowns;
    }

    public boolean isDormant() {
        return dormant.get();
    }

    public void setDormant(boolean dormant) {
        this.dormant.set(dormant);
        if (!dormant) {
            int current = this.stability.get();
            if (current < 1) {
                this.stability.compareAndSet(current, 1);
            }
        }
    }

    public boolean isCloudUnlocked() {
        return cloudUnlocked.get();
    }

    public void setCloudUnlocked(boolean cloudUnlocked) {
        this.cloudUnlocked.set(cloudUnlocked);
    }

    public boolean isWinterUnlocked() {
        return winterUnlocked.get();
    }

    public void setWinterUnlocked(boolean winterUnlocked) {
        this.winterUnlocked.set(winterUnlocked);
    }

    public boolean isShadowUnlocked() {
        return shadowUnlocked.get();
    }

    public void setShadowUnlocked(boolean shadowUnlocked) {
        this.shadowUnlocked.set(shadowUnlocked);
    }

    public boolean isStormUnlocked() {
        return stormUnlocked.get();
    }

    public void setStormUnlocked(boolean stormUnlocked) {
        this.stormUnlocked.set(stormUnlocked);
    }

    public boolean canUseAbilities() {
        return !dormant.get() && tier.get() > 0;
    }

    public StabilityState getStabilityState() {
        int stab = stability.get();
        if (stab >= 90) return StabilityState.AWAKENED;
        if (stab >= 70) return StabilityState.STABLE;
        if (stab >= 50) return StabilityState.FRACTURED;
        if (stab >= 30) return StabilityState.FADED;
        if (stab >= 1) return StabilityState.CRITICAL;
        return StabilityState.DORMANT;
    }

    public int getPassiveLevel() {
        int stab = stability.get();
        if (stab >= 90) return 2;
        if (stab >= 50) return 1;
        return 0;
    }

    public synchronized void save(ConfigurationSection section) {
        section.set("aspect", aspect.name());
        section.set("tier", tier.get());
        section.set("stability", stability.get());
        section.set("essence", essence.get());
        section.set("dormant", dormant.get());
        section.set("cloud_unlocked", cloudUnlocked.get());
        section.set("winter_unlocked", winterUnlocked.get());
        section.set("shadow_unlocked", shadowUnlocked.get());
        section.set("storm_unlocked", stormUnlocked.get());
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
        boolean shadowUnlocked = section.getBoolean("shadow_unlocked", false);
        boolean stormUnlocked = section.getBoolean("storm_unlocked", false);

        UUID owner = UUID.fromString(section.getName());
        Heart heart = new Heart(owner, aspect, tier, stability, essence, dormant);
        heart.setCloudUnlocked(cloudUnlocked);
        heart.setWinterUnlocked(winterUnlocked);
        heart.setShadowUnlocked(shadowUnlocked);
        heart.setStormUnlocked(stormUnlocked);
        
        ConfigurationSection cooldownSection = section.getConfigurationSection("cooldowns");
        if (cooldownSection != null) {
            for (String key : cooldownSection.getKeys(false)) {
                Object value = cooldownSection.get(key);
                if (value instanceof Number) {
                    heart.getCooldowns().put(key, ((Number) value).longValue());
                }
            }
        }
        return heart;
    }
}