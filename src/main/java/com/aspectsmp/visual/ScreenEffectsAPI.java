package com.aspectsmp.visual;

import com.aspectsmp.AspectSMP;
import com.aspectsmp.event.EventPhase;
import com.aspectsmp.visual.AspectVisualThemes.Theme;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.title.Title;
import net.kyori.adventure.sound.Sound;
import net.kyori.adventure.bossbar.BossBar;
import org.bukkit.*;
import org.bukkit.Color;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Vector;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.time.Duration;

public class ScreenEffectsAPI {

    private final AspectSMP plugin;
    private final VisualEffectsManager visualManager;
    private final Map<UUID, Map<String, BukkitTask>> activeEffects = new ConcurrentHashMap<>();
    private final Map<UUID, BossBar> activeBossBars = new ConcurrentHashMap<>();

    public ScreenEffectsAPI(AspectSMP plugin, VisualEffectsManager visualManager) {
        this.plugin = plugin;
        this.visualManager = visualManager;
    }

    private boolean canAffectPvP() {
        return visualManager != null && visualManager.isAffectPvP();
    }

    private boolean canScreenShake() {
        return visualManager != null && visualManager.isScreenShakeEnabled();
    }

    private boolean canVignette() {
        return visualManager != null && visualManager.isVignetteEnabled();
    }

    private boolean canChromaticAberration() {
        return visualManager != null && visualManager.isChromaticAberrationEnabled();
    }

    // ════════════════════════════════════════════════════════════════════
    // SCREEN SHAKE - Physics-based, directional, layered
    // ════════════════════════════════════════════════════════════════════

    public void screenShake(Player player, double intensity, int durationTicks) {
        if (!player.isOnline() || intensity <= 0) return;
        if (!canScreenShake() || (!canAffectPvP() && intensity > 0.5)) return; // Allow subtle shake for feedback

        String key = "shake_" + System.currentTimeMillis();
        BukkitTask task = new BukkitRunnable() {
            int ticks = 0;
            double currentIntensity = intensity;
            @Override
            public void run() {
                if (!player.isOnline() || ticks >= durationTicks) { 
                    cancel(); 
                    activeEffects.computeIfPresent(player.getUniqueId(), (k, v) -> { v.remove(key); return v; });
                    return; 
                }

                // Decay intensity over time
                currentIntensity = intensity * (1.0 - (double) ticks / durationTicks);
                
                // Multi-axis shake with smoothing
                Vector shake = new Vector(
                    (Math.random() - 0.5) * currentIntensity * 2.0,
                    (Math.random() - 0.5) * currentIntensity * 0.5,
                    (Math.random() - 0.5) * currentIntensity * 2.0
                );
                
                // Add rotational shake (pitch/yaw nudge) - ONLY if affectPvP enabled
                if (canAffectPvP()) {
                    float pitchShift = (float) ((Math.random() - 0.5) * currentIntensity * 5.0);
                    float yawShift = (float) ((Math.random() - 0.5) * currentIntensity * 5.0);
                    
                    player.setVelocity(player.getVelocity().add(shake));
                    
                    // Apply view punch via velocity manipulation
                    Location loc = player.getLocation();
                    loc.setPitch(loc.getPitch() + pitchShift);
                    loc.setYaw(loc.getYaw() + yawShift);
                    player.teleport(loc);
                } else {
                    // Cosmetic only: just velocity nudge, no view rotation
                    player.setVelocity(player.getVelocity().add(shake.multiply(0.3)));
                }
                
                ticks++;
            }
        }.runTaskTimer(plugin, 0L, 1L);

        activeEffects.computeIfAbsent(player.getUniqueId(), k -> new ConcurrentHashMap<>()).put(key, task);
    }

    public void directionalScreenShake(Player player, Location source, double intensity, int durationTicks) {
        if (!player.isOnline()) return;
        
        Vector toSource = source.toVector().subtract(player.getLocation().toVector());
        double distance = toSource.length();
        if (distance > 30) return;
        
        double falloff = 1.0 - (distance / 30.0);
        double finalIntensity = intensity * falloff;
        
        Vector direction = toSource.normalize();
        
        String key = "dir_shake_" + System.currentTimeMillis();
        BukkitTask task = new BukkitRunnable() {
            int ticks = 0;
            @Override
            public void run() {
                if (!player.isOnline() || ticks >= durationTicks) { 
                    cancel(); 
                    activeEffects.computeIfPresent(player.getUniqueId(), (k, v) -> { v.remove(key); return v; });
                    return; 
                }
                
                double currentIntensity = finalIntensity * (1.0 - (double) ticks / durationTicks);
                
                // Shake primarily in direction of source
                Vector shake = direction.clone().multiply(
                    (Math.random() - 0.3) * currentIntensity * 1.5
                ).add(new Vector(
                    (Math.random() - 0.5) * currentIntensity * 0.5,
                    (Math.random() - 0.5) * currentIntensity * 0.3,
                    (Math.random() - 0.5) * currentIntensity * 0.5
                ));
                
                player.setVelocity(player.getVelocity().add(shake));
                ticks++;
            }
        }.runTaskTimer(plugin, 0L, 1L);

        activeEffects.computeIfAbsent(player.getUniqueId(), k -> new ConcurrentHashMap<>()).put(key, task);
    }

    public void explosionScreenShake(Player player, Location center, double power, int durationTicks) {
        double distance = player.getLocation().distance(center);
        if (distance > power * 3) return;
        
        double intensity = (power / (distance + 1)) * 0.5;
        directionalScreenShake(player, center, intensity, durationTicks);
    }

    // ════════════════════════════════════════════════════════════════════
    // VIGNETTE & COLOR GRADING - Using potion effects & resource pack tricks
    // ════════════════════════════════════════════════════════════════════

    public void vignette(Player player, Color color, float intensity, int durationTicks) {
        if (!player.isOnline()) return;
        if (!canVignette() || (!canAffectPvP() && intensity > 0.3)) return; // Allow subtle cosmetic vignette
        
        // Use blindness + darkness for vignette effect (only if affectPvP enabled)
        if (canAffectPvP()) {
            int amplifier = Math.min(255, Math.max(0, (int)(intensity * 10)));
            player.addPotionEffect(new PotionEffect(
                PotionEffectType.BLINDNESS, 
                durationTicks, 
                amplifier, 
                true, false, true
            ));
            
            // Add darkness for deeper vignette
            if (intensity > 0.5) {
                player.addPotionEffect(new PotionEffect(
                    PotionEffectType.DARKNESS,
                    durationTicks,
                    (int)(intensity * 2) - 1,
                    true, false, true
                ));
            }
        }
        
        // Color tint via glowing effect (cosmetic only)
        if (intensity > 0.3) {
            player.addPotionEffect(new PotionEffect(
                PotionEffectType.GLOWING,
                durationTicks,
                0,
                true, false, false
            ));
        }
    }

    public void chromaticAberration(Player player, double intensity, int durationTicks) {
        if (!player.isOnline()) return;
        if (!canChromaticAberration() || !canAffectPvP()) return; // This effect always affects PvP
        
        // Simulate chromatic aberration with nausea + confusion
        int nauseaAmp = Math.min(4, (int)(intensity * 4));
        int confusionAmp = Math.min(2, (int)(intensity * 2));
        
        player.addPotionEffect(new PotionEffect(
            PotionEffectType.NAUSEA,
            durationTicks,
            nauseaAmp,
            true, false, true
        ));
        
        if (confusionAmp > 0) {
            player.addPotionEffect(new PotionEffect(
                PotionEffectType.CONFUSION,
                durationTicks,
                confusionAmp,
                true, false, true
            ));
        }
    }

    public void colorPulse(Player player, Color color, double intensity, int durationTicks, int pulses) {
        if (!player.isOnline()) return;
        
        new BukkitRunnable() {
            int pulse = 0;
            @Override
            public void run() {
                if (!player.isOnline() || pulse >= pulses) { cancel(); return; }
                
                // Flash screen with color using title
                player.showTitle(Title.title(
                    Component.text(" ").color(NamedTextColor.WHITE),
                    Component.text(" ").color(NamedTextColor.WHITE),
                    Title.Times.times(Duration.ofMillis(50), Duration.ofMillis(100), Duration.ofMillis(50))
                ));
                
                // Particle burst at player
                player.getWorld().spawnParticle(Particle.EXPLOSION_EMITTER, 
                    player.getLocation().add(0, 1.5, 0), 5, 1, 1, 1, 0.05);
                
                pulse++;
            }
        }.runTaskTimer(plugin, 0L, durationTicks / pulses);
    }

    // ════════════════════════════════════════════════════════════════════
    // HEARTBEAT & PULSE EFFECTS
    // ════════════════════════════════════════════════════════════════════

    public void heartbeatPulse(Player player, int stability) {
        if (!player.isOnline()) return;
        
        // Heartbeat speed based on stability (lower = faster)
        int bpm = Math.max(40, 120 - stability * 2);
        int beatInterval = 1200 / bpm; // ticks per beat
        
        String key = "heartbeat_" + System.currentTimeMillis();
        BukkitTask task = new BukkitRunnable() {
            int ticks = 0;
            @Override
            public void run() {
                if (!player.isOnline() || ticks > 200) { cancel(); return; }
                
                if (ticks % beatInterval == 0) {
                    // Screen pulse - only vignette if allowed, shake if allowed
                    float pulseIntensity = (100 - stability) / 100.0f * 0.8f + 0.2f;
                    if (canVignette()) {
                        vignette(player, Color.RED, pulseIntensity, beatInterval);
                    }
                    
                    // Sound - always play
                    player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_HEARTBEAT, 
                        pulseIntensity, 1.0f + (100 - stability) / 100.0f);
                    
                    // Subtle screen shake - only if allowed
                    if (canScreenShake()) {
                        screenShake(player, pulseIntensity * 0.3, beatInterval / 2);
                    }
                }
                ticks++;
            }
        }.runTaskTimer(plugin, 0L, 1L);

        activeEffects.computeIfAbsent(player.getUniqueId(), k -> new ConcurrentHashMap<>()).put(key, task);
    }

    public void criticalStabilityWarning(Player player, Theme theme) {
        if (!player.isOnline()) return;
        
        // Intense red vignette pulses
        for (int i = 0; i < 5; i++) {
            final int pulse = i;
            new BukkitRunnable() {
                @Override
                public void run() {
                    if (!player.isOnline()) return;
                    if (canVignette()) vignette(player, theme.getPrimaryColor(), 0.8f, 20);
                    if (canChromaticAberration() && canAffectPvP()) chromaticAberration(player, 0.5, 20);
                    if (canScreenShake()) screenShake(player, 0.5, 10);
                    player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1.0f, 0.5f);
                }
            }.runTaskLater(plugin, i * 40L);
        }
        
        // Warning title - always show
        player.showTitle(Title.title(
            Component.text("⚠ CRITICAL STABILITY ⚠").color(NamedTextColor.RED).decorate(TextDecoration.BOLD),
            Component.text("Your Heart is failing!").color(NamedTextColor.WHITE),
            Title.Times.times(Duration.ofSeconds(1), Duration.ofSeconds(3), Duration.ofSeconds(1))
        ));
    }

    public void awakenedFlash(Player player, Theme theme) {
        if (!player.isOnline()) return;
        
        Color color = theme.getPrimaryColor();
        
        // Golden/awakened color pulse
        for (int i = 0; i < 3; i++) {
            final int pulse = i;
            new BukkitRunnable() {
                @Override
                public void run() {
                    if (!player.isOnline()) return;
                    colorPulse(player, color, 0.6, 20, 1);
                    player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.5f);
                }
            }.runTaskLater(plugin, pulse * 30L);
        }
        
        player.showTitle(Title.title(
            Component.text("✦ AWAKENED ✦").color(NamedTextColor.GOLD).decorate(TextDecoration.BOLD),
            Component.text("Your Heart reaches its true potential!").color(NamedTextColor.YELLOW),
            Title.Times.times(Duration.ofSeconds(1), Duration.ofSeconds(3), Duration.ofSeconds(1))
        ));
    }

    public void heartDeath(Player player, Theme theme) {
        if (!player.isOnline()) return;
        
        // Dramatic death sequence
        if (canVignette()) vignette(player, Color.BLACK, 1.0f, 100);
        if (canChromaticAberration() && canAffectPvP()) chromaticAberration(player, 1.0, 100);
        if (canScreenShake()) screenShake(player, 2.0, 60);
        
        player.showTitle(Title.title(
            Component.text("☠ HEART DORMANT ☠").color(NamedTextColor.DARK_RED).decorate(TextDecoration.BOLD),
            Component.text("Your Heart has gone dormant...").color(NamedTextColor.GRAY),
            Title.Times.times(Duration.ofSeconds(2), Duration.ofSeconds(4), Duration.ofSeconds(2))
        ));
        
        player.playSound(player.getLocation(), Sound.ENTITY_WITHER_DEATH, 1.0f, 0.5f);
    }

    // ════════════════════════════════════════════════════════════════════
    // ABILITY CAST & IMPACT EFFECTS
    // ════════════════════════════════════════════════════════════════════

    public void chargeUp(Player player, Theme theme, int tier) {
        if (!player.isOnline()) return;
        
        int chargeTime = tier * 10; // 10, 20, 30 ticks
        Color color = theme.getPrimaryColor();
        
        new BukkitRunnable() {
            int ticks = 0;
            @Override
            public void run() {
                if (!player.isOnline() || ticks >= chargeTime) { cancel(); return; }
                
                float progress = (float) ticks / chargeTime;
                
                // Increasing vignette intensity
                if (canVignette()) vignette(player, color, progress * 0.5f, 2);
                
                // Charge sound (rising pitch)
                float pitch = 0.5f + progress * 1.5f;
                player.playSound(player.getLocation(), theme.getCastSound(), 0.3f, pitch);
                
                // Particles converging
                player.getWorld().spawnParticle(theme.getCastParticle(),
                    player.getLocation().add(0, 1, 0),
                    (int)(5 * progress * tier), 0.5, 0.5, 0.5, 0.02 * progress);
                
                ticks++;
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }

    public void impactFlash(Player player, Theme theme, int tier) {
        if (!player.isOnline()) return;
        
        Color color = theme.getPrimaryColor();
        float intensity = 0.3f + tier * 0.2f;
        
        if (canVignette()) vignette(player, color, intensity, 10);
        if (canChromaticAberration() && canAffectPvP()) chromaticAberration(player, intensity * 0.5, 10);
        
        // White flash
        player.showTitle(Title.title(
            Component.text(" ").color(NamedTextColor.WHITE),
            Component.text(" ").color(NamedTextColor.WHITE),
            Title.Times.times(Duration.ofMillis(10), Duration.ofMillis(50), Duration.ofMillis(10))
        ));
    }

    public void ultimateScreenFlash(Player player, Theme theme) {
        if (!player.isOnline()) return;
        
        Color primary = theme.getPrimaryColor();
        Color accent = theme.getAccentColor();
        
        // Multi-stage ultimate flash
        new BukkitRunnable() {
            int stage = 0;
            @Override
            public void run() {
                if (!player.isOnline() || stage >= 6) { cancel(); return; }
                
                switch (stage) {
                    case 0 -> {
                        // Black flash
                        if (canVignette()) vignette(player, Color.BLACK, 1.0f, 5);
                        if (canChromaticAberration() && canAffectPvP()) chromaticAberration(player, 1.0, 5);
                    }
                    case 1 -> {
                        // Primary color flash
                        colorPulse(player, primary, 1.0, 10, 1);
                    }
                    case 2 -> {
                        // Accent color flash
                        colorPulse(player, accent, 1.0, 10, 1);
                    }
                    case 3 -> {
                        // White flash
                        colorPulse(player, Color.WHITE, 1.0, 10, 1);
                    }
                    case 4 -> {
                        // Primary again
                        if (canVignette()) vignette(player, primary, 0.8f, 20);
                    }
                    case 5 -> {
                        // Fade out
                        player.playSound(player.getLocation(), theme.getUltimateSound(), 2.0f, 0.8f);
                    }
                }
                stage++;
            }
        }.runTaskTimer(plugin, 0L, 5L);
        
        // Epic title
        player.showTitle(Title.title(
            Component.text("ULTIMATE").color(NamedTextColor.WHITE).decorate(TextDecoration.BOLD),
            Component.text(" ").color(NamedTextColor.WHITE),
            Title.Times.times(Duration.ofMillis(200), Duration.ofMillis(1000), Duration.ofMillis(300))
        ));
    }

    public void hitFlash(Player player, Theme theme) {
        if (!player.isOnline()) return;
        
        // Quick red flash on hit - only cosmetic if not affectPvP
        if (canVignette()) vignette(player, Color.RED, 0.6f, 5);
        if (canChromaticAberration() && canAffectPvP()) chromaticAberration(player, 0.3, 5);
        if (canScreenShake()) screenShake(player, 0.4, 5);
    }

    public void killFlash(Player player, Theme theme) {
        if (!player.isOnline()) return;
        
        // Satisfying kill flash - cosmetic only
        colorPulse(player, theme.getAccentColor(), 0.7, 15, 2);
        player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 2.0f);
    }

    public void deathScreen(Player player, Theme theme) {
        if (!player.isOnline()) return;
        
        // Dramatic death sequence
        if (canVignette()) vignette(player, Color.BLACK, 1.0f, 100);
        if (canChromaticAberration() && canAffectPvP()) chromaticAberration(player, 0.8, 100);
        
        player.showTitle(Title.title(
            Component.text("YOU DIED").color(NamedTextColor.DARK_RED).decorate(TextDecoration.BOLD),
            Component.text("Your Heart flickers...").color(NamedTextColor.GRAY),
            Title.Times.times(Duration.ofSeconds(2), Duration.ofSeconds(4), Duration.ofSeconds(2))
        ));
    }

    public void tierUpFlash(Player player, Theme theme, int newTier) {
        if (!player.isOnline()) return;
        
        String[] tierNames = {"", "TIER I", "TIER II", "TIER III"};
        Color color = theme.getPrimaryColor();
        
        // Tier up animation
        for (int i = 0; i < newTier; i++) {
            final int pulse = i;
            new BukkitRunnable() {
                @Override
                public void run() {
                    if (!player.isOnline()) return;
                    colorPulse(player, color, 0.8, 15, 1);
                    if (canScreenShake()) screenShake(player, 0.5, 10);
                }
            }.runTaskLater(plugin, pulse * 25L);
        }
        
        player.showTitle(Title.title(
            Component.text(tierNames[newTier]).color(NamedTextColor.GOLD).decorate(TextDecoration.BOLD),
            Component.text("Heart Ascended!").color(NamedTextColor.YELLOW),
            Title.Times.times(Duration.ofSeconds(1), Duration.ofSeconds(3), Duration.ofSeconds(1))
        ));
        
        player.playSound(player.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 1.0f, 1.0f + newTier * 0.2f);
    }

    // ════════════════════════════════════════════════════════════════════
    // ASPECT TRANSITION
    // ════════════════════════════════════════════════════════════════════

    public void aspectTransition(Player player, Theme oldTheme, Theme newTheme) {
        if (!player.isOnline()) return;
        
        // Dramatic transition sequence
        // 1. Old aspect fades
        if (canVignette()) vignette(player, oldTheme.getPrimaryColor(), 0.8f, 20);
        if (canChromaticAberration() && canAffectPvP()) chromaticAberration(player, 0.8, 20);
        
        new BukkitRunnable() {
            @Override
            public void run() {
                if (!player.isOnline()) return;
                
                // 2. Void moment
                if (canVignette()) vignette(player, Color.BLACK, 1.0f, 10);
                if (canScreenShake()) screenShake(player, 1.5, 20);
                player.playSound(player.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, 2.0f, 0.3f);
                
                new BukkitRunnable() {
                    @Override
                    public void run() {
                        if (!player.isOnline()) return;
                        
                        // 3. New aspect emerges
                        for (int i = 0; i < 3; i++) {
                            final int p = i;
                            new BukkitRunnable() {
                                @Override
                                public void run() {
                                    if (!player.isOnline()) return;
                                    colorPulse(player, newTheme.getPrimaryColor(), 0.8, 15, 1);
                                }
                            }.runTaskLater(plugin, p * 20L);
                        }
                        
                        player.showTitle(Title.title(
                            Component.text("ASPECT SHIFT").color(NamedTextColor.WHITE).decorate(TextDecoration.BOLD),
                            Component.text("Now bound to " + newTheme.getAspect().getDisplayName()).color(newTheme.getPrimaryColor()),
                            Title.Times.times(Duration.ofSeconds(1), Duration.ofSeconds(3), Duration.ofSeconds(1))
                        ));
                        
                        player.playSound(player.getLocation(), newTheme.getCastSound(), 1.5f, 1.2f);
                    }
                }.runTaskLater(plugin, 15L);
            }
        }.runTaskLater(plugin, 20L);
    }

    // ════════════════════════════════════════════════════════════════════
    // EVENT EFFECTS
    // ════════════════════════════════════════════════════════════════════

    public void eventPhaseTransition(Player player, Theme theme, EventPhase phase) {
        if (!player.isOnline()) return;
        
        String phaseName = formatPhaseName(phase);
        Color color = theme.getPrimaryColor();
        
        // Phase transition cinematic
        if (canVignette()) vignette(player, color, 0.7f, 40);
        if (canChromaticAberration() && canAffectPvP()) chromaticAberration(player, 0.5, 40);
        if (canScreenShake()) screenShake(player, 1.0, 30);
        
        player.showTitle(Title.title(
            Component.text(phaseName).color(NamedTextColor.WHITE).decorate(TextDecoration.BOLD),
            Component.text("The " + theme.getAspect().getDisplayName() + " event evolves...").color(color),
            Title.Times.times(Duration.ofSeconds(2), Duration.ofSeconds(4), Duration.ofSeconds(2))
        ));
        
        player.playSound(player.getLocation(), theme.getUltimateSound(), 1.5f, 0.8f);
    }

    public void globalAnnouncement(Player player, String title, String subtitle, Color color) {
        if (!player.isOnline()) return;
        
        player.showTitle(Title.title(
            Component.text(title).color(color).decorate(TextDecoration.BOLD),
            Component.text(subtitle).color(NamedTextColor.WHITE),
            Title.Times.times(Duration.ofSeconds(2), Duration.ofSeconds(5), Duration.ofSeconds(2))
        ));
        
        // Subtle screen effects
        if (canVignette()) vignette(player, color, 0.4f, 60);
        if (canChromaticAberration() && canAffectPvP()) chromaticAberration(player, 0.2, 60);
    }

    // ════════════════════════════════════════════════════════════════════
    // BOSS BARS
    // ════════════════════════════════════════════════════════════════════

    public void showBossBar(Player player, String id, String name, float progress, Color color, BossBar.Overlay... overlays) {
        if (!player.isOnline()) return;
        
        BossBar bar = BossBar.bossBar(
            Component.text(name).color(NamedTextColor.WHITE).decorate(TextDecoration.BOLD),
            progress,
            mapColor(color),
            overlays
        );
        player.showBossBar(bar);
        activeBossBars.put(UUID.fromString(id), bar);
    }

    public void updateBossBar(Player player, String id, float progress, String name) {
        BossBar bar = activeBossBars.get(UUID.fromString(id));
        if (bar != null) {
            bar.progress(progress);
            if (name != null) bar.name(Component.text(name).color(NamedTextColor.WHITE).decorate(TextDecoration.BOLD));
        }
    }

    public void removeBossBar(Player player, String id) {
        BossBar bar = activeBossBars.remove(UUID.fromString(id));
        if (bar != null) player.hideBossBar(bar);
    }

    // ════════════════════════════════════════════════════════════════════
    // CLEAR EFFECTS
    // ════════════════════════════════════════════════════════════════════

    public void clearAll(Player player) {
        if (!player.isOnline()) return;
        
        // Clear potion effects used for screen effects
        player.removePotionEffect(PotionEffectType.BLINDNESS);
        player.removePotionEffect(PotionEffectType.DARKNESS);
        player.removePotionEffect(PotionEffectType.NAUSEA);
        player.removePotionEffect(PotionEffectType.CONFUSION);
        player.removePotionEffect(PotionEffectType.GLOWING);
        
        // Cancel active tasks
        Map<String, BukkitTask> effects = activeEffects.remove(player.getUniqueId());
        if (effects != null) {
            effects.values().forEach(BukkitTask::cancel);
        }
        
        // Hide boss bars
        activeBossBars.values().forEach(bar -> player.hideBossBar(bar));
        activeBossBars.clear();
    }

    // ════════════════════════════════════════════════════════════════════
    // UTILITIES
    // ════════════════════════════════════════════════════════════════════

    private String formatPhaseName(EventPhase phase) {
        String name = phase.name().replace("_", " ");
        return Arrays.stream(name.toLowerCase().split(" "))
            .map(w -> Character.toUpperCase(w.charAt(0)) + w.substring(1))
            .reduce("", (a, b) -> a + " " + b).trim();
    }

    private BossBar.Color mapColor(Color color) {
        // Map Bukkit Color to Adventure BossBar.Color
        int rgb = color.asRGB();
        int r = (rgb >> 16) & 0xFF;
        int g = (rgb >> 8) & 0xFF;
        int b = rgb & 0xFF;
        
        // Find closest boss bar color
        if (r > 200 && g < 100 && b < 100) return BossBar.Color.RED;
        if (r < 100 && g < 100 && b > 200) return BossBar.Color.BLUE;
        if (r < 100 && g > 200 && b < 100) return BossBar.Color.GREEN;
        if (r > 200 && g > 200 && b < 100) return BossBar.Color.YELLOW;
        if (r > 100 && g < 100 && b > 100) return BossBar.Color.PURPLE;
        return BossBar.Color.WHITE;
    }
}