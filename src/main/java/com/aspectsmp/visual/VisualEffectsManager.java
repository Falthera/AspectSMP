package com.aspectsmp.visual;

import com.aspectsmp.AspectSMP;
import com.aspectsmp.core.AspectType;
import com.aspectsmp.core.Heart;
import com.aspectsmp.event.EventPhase;
import org.bukkit.*;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Vector;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

public class VisualEffectsManager {

    private final AspectSMP plugin;
    private final Map<UUID, BukkitTask> passiveEffectTasks = new ConcurrentHashMap<>();
    private final Map<UUID, BukkitTask> ambientTrailTasks = new ConcurrentHashMap<>();
    private final Map<UUID, BukkitTask> orbitEffectTasks = new ConcurrentHashMap<>();
    private final Map<UUID, BukkitTask> trailEffectTasks = new ConcurrentHashMap<>();
    private final Map<UUID, Map<String, Object>> playerEffectState = new ConcurrentHashMap<>();
    private final AspectVisualThemes themes;
    private final ScreenEffectsAPI screenEffects;
    private final ParticleEffects particles;
    private final DynamicSoundscapes soundscapes;

    private boolean enabled = true;
    private int particleQuality = 3; // 1=low, 2=medium, 3=high, 4=ultra
    private boolean screenShakeEnabled = true;
    private boolean vignetteEnabled = true;
    private boolean chromaticAberrationEnabled = true;
    private boolean affectPvP = false; // When false, disables effects that modify player view/movement

    public VisualEffectsManager(AspectSMP plugin) {
        this.plugin = plugin;
        this.themes = new AspectVisualThemes();
        this.screenEffects = new ScreenEffectsAPI(plugin, this);
        this.particles = new ParticleEffects(plugin);
        this.soundscapes = new DynamicSoundscapes(plugin);
        loadConfig();
    }

    public void loadConfig() {
        this.enabled = plugin.getConfig().getBoolean("visual-effects.enabled", true);
        this.particleQuality = plugin.getConfig().getInt("visual-effects.particle-quality", 3);
        this.screenShakeEnabled = plugin.getConfig().getBoolean("visual-effects.screen-shake", true);
        this.vignetteEnabled = plugin.getConfig().getBoolean("visual-effects.vignette", true);
        this.chromaticAberrationEnabled = plugin.getConfig().getBoolean("visual-effects.chromatic-aberration", true);
        this.affectPvP = plugin.getConfig().getBoolean("visual-effects.affect-pvp", false);
    }

    public void saveConfig() {
        plugin.getConfig().set("visual-effects.enabled", enabled);
        plugin.getConfig().set("visual-effects.particle-quality", particleQuality);
        plugin.getConfig().set("visual-effects.screen-shake", screenShakeEnabled);
        plugin.getConfig().set("visual-effects.vignette", vignetteEnabled);
        plugin.getConfig().set("visual-effects.chromatic-aberration", chromaticAberrationEnabled);
        plugin.getConfig().set("visual-effects.affect-pvp", affectPvP);
        plugin.saveConfig();
    }

    public boolean isAffectPvP() { return affectPvP; }
    public void setAffectPvP(boolean affectPvP) { this.affectPvP = affectPvP; }

    public void startPassiveEffects(Player player) {
        if (!enabled) return;
        
        // Use async API properly - join on the future
        CompletableFuture<Heart> heartFuture = plugin.getHeartManager().getHeart(player);
        if (heartFuture == null) return;
        
        Heart heart;
        try {
            heart = heartFuture.join();
        } catch (Exception e) {
            return;
        }
        
        if (heart == null || heart.isDormant()) return;

        AspectType aspect = heart.getAspect();
        AspectVisualThemes.Theme theme = themes.getTheme(aspect);

        // Cancel any existing tasks before starting new ones
        stopPassiveEffects(player);

        // Use the new advanced passive aura system
        particles.startPassiveAura(player, theme, heart.getStability());
        
        // Start orbit effect based on tier
        if (heart.getTier() >= 2) {
            startOrbitEffect(player, theme, heart.getTier());
        }
        
        // Start trail effect
        startTrailEffect(player, theme);

        // Start ambient soundscape
        soundscapes.startAmbientSoundscape(player, theme, heart.getStability());

        // Periodic stability checks
        BukkitTask passiveTask = new BukkitRunnable() {
            int tick = 0;
            @Override
            public void run() {
                if (!player.isOnline() || !enabled) { cancel(); return; }
                
                CompletableFuture<Heart> currentHeartFuture = plugin.getHeartManager().getHeart(player);
                if (currentHeartFuture == null) { cancel(); return; }
                
                Heart currentHeart;
                try {
                    currentHeart = currentHeartFuture.join();
                } catch (Exception e) {
                    cancel();
                    return;
                }
                
                if (currentHeart == null || currentHeart.isDormant()) {
                    cancel();
                    return;
                }
                
                // Update aspect and theme if changed
                AspectType currentAspect = currentHeart.getAspect();
                AspectVisualThemes.Theme currentTheme = themes.getTheme(currentAspect);
                
                tick++;
                int stability = currentHeart.getStability();

                // Heartbeat pulse at low stability
                if (stability < 40 && tick % 40 == 0) {
                    screenEffects.heartbeatPulse(player, stability);
                    particles.heartBeatParticles(player, currentTheme, stability);
                    soundscapes.playHeartbeat(player, currentTheme, stability);
                }

                // Essence visual indicator
                if (currentHeart.getEssence() > 50 && tick % 60 == 0) {
                    particles.spawnEssenceGainParticles(player.getLocation(), currentTheme, (int)(currentHeart.getEssence() / 10), particleQuality);
                }

                // Tier visual indicators
                if (currentHeart.getTier() >= 2 && tick % 120 == 0) {
                    spawnTierIndicator(player.getLocation(), currentTheme, currentHeart.getTier());
                }
                
                // Critical stability warning
                if (stability < 20 && tick % 100 == 0) {
                    screenEffects.criticalStabilityWarning(player, currentTheme);
                    soundscapes.playCriticalWarningSound(player, currentTheme);
                }

                // Awakened check
                if (stability >= 100 && tick % 200 == 0) {
                    particles.spawnAwakenedAura(player.getLocation(), currentTheme, particleQuality);
                }
            }
        }.runTaskTimer(plugin, 0L, 1L);

        passiveEffectTasks.put(player.getUniqueId(), passiveTask);

        // Start ambient trail task (legacy trail)
        startAmbientTrail(player, theme);
    }

    private void startAmbientTrail(Player player, AspectVisualThemes.Theme theme) {
        // Don't create duplicate task if one already exists
        if (ambientTrailTasks.containsKey(player.getUniqueId())) {
            return;
        }
        
        BukkitTask trailTask = new BukkitRunnable() {
            Location lastLoc = player.getLocation();
            @Override
            public void run() {
                if (!player.isOnline() || !enabled) { cancel(); return; }
                
                Location currentLoc = player.getLocation();
                if (currentLoc.distanceSquared(lastLoc) > 0.25) {
                    Vector direction = currentLoc.toVector().subtract(lastLoc.toVector()).normalize();
                    spawnTrailSegment(lastLoc, currentLoc, theme, particleQuality);
                    lastLoc = currentLoc.clone();
                }
            }
        }.runTaskTimer(plugin, 0L, 1L);

        ambientTrailTasks.put(player.getUniqueId(), trailTask);
    }

    public void startOrbitEffect(Player player, AspectVisualThemes.Theme theme, int tier) {
        UUID id = player.getUniqueId();
        // Cancel existing orbit task if any
        stopOrbitEffect(player);
        
        BukkitTask task = new BukkitRunnable() {
            double angle = 0;
            double verticalAngle = 0;
            int tick = 0;
            @Override
            public void run() {
                if (!player.isOnline() || player.isDead()) { cancel(); return; }

                Location loc = player.getLocation().add(0, 1.5, 0);
                int maxParticles = 4 + tier;
                int particlesPerTick = Math.max(1, maxParticles / 2);
                
                for (int i = 0; i < particlesPerTick; i++) {
                    int idx = (tick + i) % maxParticles;
                    double a = angle + (idx * Math.PI * 2 / maxParticles);
                    double radius = 1.0 + tier * 0.3;
                    double y = Math.sin(verticalAngle + idx) * 0.5;
                    
                    Vector v = new Vector(Math.cos(a) * radius, y, Math.sin(a) * radius);
                    loc.add(v);
                    player.getWorld().spawnParticle(theme.getOrbitParticle(), loc, 1, 0, 0, 0, theme.getOrbitSpeed());
                    loc.subtract(v);
                }

                angle += theme.getOrbitSpeed();
                verticalAngle += 0.02;
                tick++;
            }
        }.runTaskTimer(plugin, 0L, 1L);

        orbitEffectTasks.put(id, task);
    }

    public void stopOrbitEffect(Player player) {
        BukkitTask task = orbitEffectTasks.remove(player.getUniqueId());
        if (task != null) task.cancel();
        particles.stopOrbitEffect(player.getUniqueId());
    }

    public void startTrailEffect(Player player, AspectVisualThemes.Theme theme) {
        UUID id = player.getUniqueId();
        // Cancel existing trail task if any
        stopTrailEffect(player);
        
        Location[] lastLoc = { player.getLocation() };

        BukkitTask task = new BukkitRunnable() {
            int tick = 0;
            @Override
            public void run() {
                if (!player.isOnline() || player.isDead()) { cancel(); return; }

                Location loc = player.getLocation();
                
                // Only spawn if moved
                if (loc.distance(lastLoc[0]) > 0.2) {
                    int trailCount = Math.min(theme.getTrailParticleCount(), 3);
                    for (int i = 0; i < trailCount; i++) {
                        Vector offset = new Vector(
                            (Math.random() - 0.5) * 0.3,
                            Math.random() * 1.0,
                            (Math.random() - 0.5) * 0.3
                        );
                        loc.add(offset);
                        player.getWorld().spawnParticle(theme.getTrailParticle(), loc, 1, 0, 0, 0, theme.getTrailSpeed());
                        loc.subtract(offset);
                    }
                    
                    // TRAIL particle toward last location (every 3 ticks)
                    if (theme.getTrailParticleAlt() != null && tick % 3 == 0) {
                        // Use particle builder API
                        loc.getWorld().spawnParticle(theme.getTrailParticleAlt(), loc, 1, 0, 0, 0, 0);
                    }
                    
                    lastLoc[0] = loc.clone();
                }
                tick++;
            }
        }.runTaskTimer(plugin, 0L, 2L);

        trailEffectTasks.put(id, task);
    }

    public void stopTrailEffect(Player player) {
        BukkitTask task = trailEffectTasks.remove(player.getUniqueId());
        if (task != null) task.cancel();
        particles.stopTrailEffect(player.getUniqueId());
    }

    public void stopPassiveEffects(Player player) {
        BukkitTask passive = passiveEffectTasks.remove(player.getUniqueId());
        if (passive != null) passive.cancel();

        BukkitTask trail = ambientTrailTasks.remove(player.getUniqueId());
        if (trail != null) trail.cancel();

        stopOrbitEffect(player);
        stopTrailEffect(player);

        particles.clearPlayer(player);
        soundscapes.clearPlayer(player);
        screenEffects.clearAll(player);
    }

    private void spawnAmbientAura(Player player, AspectVisualThemes.Theme theme, double stabilityMultiplier) {
        int count = (int) (theme.getAuraParticleCount() * stabilityMultiplier * particleQuality / 3.0);
        if (count < 1) return;

        Location loc = player.getLocation().add(0, 1, 0);
        for (int i = 0; i < count; i++) {
            double angle = Math.random() * 2 * Math.PI;
            double radius = 0.5 + Math.random() * 1.5;
            double height = Math.random() * 2.0;
            
            Location particleLoc = loc.clone().add(
                Math.cos(angle) * radius,
                height,
                Math.sin(angle) * radius
            );
            
            player.getWorld().spawnParticle(theme.getAuraParticle(), particleLoc, 1, 0, 0, 0, theme.getAuraSpeed());
        }
    }

    private void spawnOrbitingParticles(Player player, AspectVisualThemes.Theme theme, double stabilityMultiplier) {
        if (!playerEffectState.containsKey(player.getUniqueId())) {
            playerEffectState.put(player.getUniqueId(), new ConcurrentHashMap<>());
        }
        Map<String, Object> state = playerEffectState.get(player.getUniqueId());
        
        double orbitAngle = ((double) state.getOrDefault("orbitAngle", 0.0)) + 0.15;
        state.put("orbitAngle", orbitAngle % (2 * Math.PI));
        
        int rings = 2 + particleQuality;
        int particlesPerRing = 3 * particleQuality;
        double radius = 1.2 + (particleQuality * 0.2);

        Location center = player.getLocation().add(0, 1.2, 0);
        
        for (int ring = 0; ring < rings; ring++) {
            double ringRadius = radius * (0.6 + ring * 0.3);
            double ringHeight = 0.5 + ring * 0.4;
            double phaseOffset = ring * Math.PI / rings;
            
            for (int i = 0; i < particlesPerRing; i++) {
                double angle = orbitAngle + phaseOffset + (2 * Math.PI * i / particlesPerRing);
                Location particleLoc = center.clone().add(
                    Math.cos(angle) * ringRadius,
                    ringHeight,
                    Math.sin(angle) * ringRadius
                );
                
                player.getWorld().spawnParticle(
                    theme.getOrbitParticle(),
                    particleLoc,
                    1, 0, 0, 0, theme.getOrbitSpeed()
                );
            }
        }
    }

    private void spawnMovementTrail(Player player, AspectVisualThemes.Theme theme, double stabilityMultiplier) {
        Location loc = player.getLocation();
        int count = (int) (theme.getTrailParticleCount() * stabilityMultiplier * particleQuality / 3.0);
        
        for (int i = 0; i < count; i++) {
            Location particleLoc = loc.clone().add(
                (Math.random() - 0.5) * 0.6,
                Math.random() * 0.5,
                (Math.random() - 0.5) * 0.6
            );
            player.getWorld().spawnParticle(theme.getTrailParticle(), particleLoc, 1, 0, 0, 0, theme.getTrailSpeed());
        }
    }

    private void spawnTrailSegment(Location from, Location to, AspectVisualThemes.Theme theme, int quality) {
        Vector direction = to.toVector().subtract(from.toVector());
        double distance = direction.length();
        if (distance < 0.1) return;
        direction.normalize();
        
        int segments = (int) (distance * 5 * quality / 3.0);
        for (int i = 0; i <= segments; i++) {
            double progress = (double) i / segments;
            Location point = from.clone().add(direction.clone().multiply(distance * progress));
            point.add((Math.random() - 0.5) * 0.3, Math.random() * 0.5, (Math.random() - 0.5) * 0.3);
            point.getWorld().spawnParticle(theme.getTrailParticle(), point, 1, 0, 0, 0, theme.getTrailSpeed());
        }
    }

    private void spawnTierIndicator(Location location, AspectVisualThemes.Theme theme, int tier) {
        Color color = theme.getPrimaryColor();
        for (int i = 0; i < tier; i++) {
            double angle = i * Math.PI * 2 / tier;
            double radius = 2.0;
            Location loc = location.clone().add(Math.cos(angle) * radius, 2.0, Math.sin(angle) * radius);
            loc.getWorld().spawnParticle(theme.getOrbitParticle(), loc, 5, 0.2, 0.2, 0.2, 0.02);
        }
    }

    // ════════════════════════════════════════════════════════════════════
    // ABILITY VISUALS - INSANE EDITION
    // ════════════════════════════════════════════════════════════════════

    public void playAbilityCastVisuals(Player player, String abilityId, AspectType aspect, int tier, boolean isUltimate) {
        if (!enabled) return;

        AspectVisualThemes.Theme theme = themes.getTheme(aspect);
        Heart heart = plugin.getHeartManager().getHeart(player.getUniqueId()).orElse(null);
        double power = heart != null ? Math.max(0.5, heart.getStability() / 100.0) : 1.0;

        // Pre-cast buildup
        screenEffects.chargeUp(player, theme, tier);
        particles.castCharge(player, theme, tier, tier * 10);
        soundscapes.playCastSound(player, theme, tier, null);

        // Cast particles
        particles.castRelease(player, theme, tier);

        // Screen effects
        if (tier >= 2) {
            screenEffects.impactFlash(player, theme, tier);
            if (screenShakeEnabled) {
                screenEffects.screenShake(player, 0.3 * tier, 10 * tier);
            }
        }

        // Ultimate effects - only for actual ultimates
        if (isUltimate) {
            screenEffects.ultimateScreenFlash(player, theme);
            if (screenShakeEnabled) {
                screenEffects.screenShake(player, 1.5, 30);
            }
            soundscapes.playUltimateSound(player, theme);
            particles.ultimate(player, theme);
        }
    }

    public void playUltimateActivationVisuals(Player player, AspectType aspect, String abilityId) {
        if (!enabled) return;

        AspectVisualThemes.Theme theme = themes.getTheme(aspect);
        Location center = player.getLocation().add(0, 1, 0);
        World world = center.getWorld();

        switch (abilityId) {
            case "winter:absolute_zero":
                spawnUltimateRing(world, center, theme, 0.0, Particle.SNOWFLAKE, 3.0, 50);
                spawnUltimateRing(world, center, theme, 1.0, Particle.END_ROD, 2.0, 30);
                spawnUltimateColumn(world, center, theme, Particle.SNOWFLAKE, 4.0, 60, 0.02);
                spawnUltimateColumn(world, center, theme, Particle.END_ROD, 3.0, 20, 0.05);
                soundscapes.playUltimateSound(player, theme);
                screenEffects.ultimateScreenFlash(player, theme);
                break;

            case "shadow:void_reaper":
                spawnUltimateSpiral(world, center, Particle.SOUL_FIRE_FLAME, 5.0, 30, 0.15);
                spawnUltimateSpiral(world, center, Particle.PORTAL, 4.0, 20, 0.1);
                spawnUltimateColumn(world, center, theme, Particle.SMOKE, 6.0, 40, 0.1);
                soundscapes.playUltimateSound(player, theme);
                screenEffects.ultimateScreenFlash(player, theme);
                break;

            case "cloud:cloudburst":
                spawnUltimateRing(world, center, theme, 0.0, Particle.CLOUD, 6.0, 60);
                spawnUltimateColumn(world, center, theme, Particle.EXPLOSION_EMITTER, 0, 15, 0.1);
                spawnUltimateRing(world, center, theme, 0.0, Particle.END_ROD, 5.0, 40);
                soundscapes.playUltimateSound(player, theme);
                screenEffects.ultimateScreenFlash(player, theme);
                break;

            case "cosmos:black_star_collapse":
                spawnUltimateSpiral(world, center, Particle.PORTAL, 5.0, 40, 0.12);
                spawnUltimateSpiral(world, center, Particle.END_ROD, 4.0, 25, 0.1);
                spawnUltimateColumn(world, center, theme, Particle.END_ROD, 5.0, 30, 0.08);
                soundscapes.playUltimateSound(player, theme);
                screenEffects.ultimateScreenFlash(player, theme);
                break;

            case "cosmos:meteor_call":
                spawnUltimateRing(world, center, theme, 0.0, Particle.FLAME, 6.0, 50);
                spawnUltimateSpiral(world, center, Particle.LAVA, 5.0, 30, 0.1);
                spawnUltimateColumn(world, center, theme, Particle.SMOKE, 4.0, 35, 0.08);
                soundscapes.playUltimateSound(player, theme);
                screenEffects.ultimateScreenFlash(player, theme);
                break;

            case "inferno:sun_core_collapse":
                spawnUltimateColumn(world, center, theme, Particle.FLAME, 7.0, 60, 0.05);
                spawnUltimateRing(world, center, theme, 0.0, Particle.FLAME, 5.0, 40);
                spawnUltimateSpiral(world, center, Particle.LAVA, 4.0, 25, 0.12);
                soundscapes.playUltimateSound(player, theme);
                screenEffects.ultimateScreenFlash(player, theme);
                break;

            case "tempest:thunder_execution":
                spawnUltimateSpiral(world, center, Particle.ELECTRIC_SPARK, 4.0, 50, 0.1);
                spawnUltimateColumn(world, center, theme, Particle.ELECTRIC_SPARK, 6.0, 40, 0.05);
                spawnUltimateRing(world, center, theme, 0.0, Particle.END_ROD, 5.0, 30);
                soundscapes.playUltimateSound(player, theme);
                screenEffects.ultimateScreenFlash(player, theme);
                break;

            case "war:war_god_state":
                spawnUltimateColumn(world, center, theme, Particle.FLAME, 8.0, 50, 0.05);
                spawnUltimateRing(world, center, theme, 0.0, Particle.FLAME, 6.0, 40);
                spawnUltimateSpiral(world, center, Particle.EXPLOSION_EMITTER, 5.0, 10, 0.15);
                soundscapes.playUltimateSound(player, theme);
                screenEffects.ultimateScreenFlash(player, theme);
                break;

            case "rift:reality_collapse":
                spawnUltimateSpiral(world, center, Particle.PORTAL, 5.0, 40, 0.12);
                spawnUltimateColumn(world, center, theme, Particle.END_ROD, 4.0, 30, 0.08);
                spawnUltimateRing(world, center, theme, 0.0, Particle.SMOKE, 6.0, 30);
                soundscapes.playUltimateSound(player, theme);
                screenEffects.ultimateScreenFlash(player, theme);
                break;

            case "vitality:genesis_field":
                spawnUltimateSpiral(world, center, Particle.HEART, 5.0, 35, 0.1);
                spawnUltimateRing(world, center, theme, 0.0, Particle.HEART, 4.0, 40);
                spawnUltimateColumn(world, center, theme, Particle.HAPPY_VILLAGER, 3.0, 45, 0.08);
                soundscapes.playUltimateSound(player, theme);
                screenEffects.ultimateScreenFlash(player, theme);
                break;

            case "tide:maelstrom":
                spawnUltimateColumn(world, center, theme, Particle.BUBBLE_COLUMN_UP, 6.0, 50, 0.05);
                spawnUltimateRing(world, center, theme, 0.0, Particle.SPLASH, 5.0, 35);
                spawnUltimateSpiral(world, center, Particle.SPLASH, 4.0, 25, 0.12);
                soundscapes.playUltimateSound(player, theme);
                screenEffects.ultimateScreenFlash(player, theme);
                break;

            default:
                soundscapes.playUltimateSound(player, theme);
                screenEffects.ultimateScreenFlash(player, theme);
                particles.ultimate(player, theme);
                break;
        }
    }

    private void spawnUltimateRing(World world, Location center, AspectVisualThemes.Theme theme, double yOffset, Particle particle, double radius, int count) {
        for (int i = 0; i < count; i++) {
            double angle = Math.PI * 2 * i / count;
            Location loc = center.clone().add(Math.cos(angle) * radius, yOffset + Math.sin(angle * 2) * 0.3, Math.sin(angle) * radius);
            world.spawnParticle(particle, loc, 1, 0, 0, 0, 0);
        }
    }

    private void spawnUltimateColumn(World world, Location center, AspectVisualThemes.Theme theme, Particle particle, double radius, int height, double speed) {
        for (int y = 0; y <= height; y++) {
            for (int i = 0; i < 8; i++) {
                double angle = Math.PI * 2 * i / 8;
                Location loc = center.clone().add(Math.cos(angle) * radius, y * 0.25, Math.sin(angle) * radius);
                world.spawnParticle(particle, loc, 1, 0, 0, 0, speed);
            }
        }
    }

    private void spawnUltimateSpiral(World world, Location center, Particle particle, double maxRadius, int turns, double verticalSpeed) {
        for (int i = 0; i < turns; i++) {
            double angle = Math.PI * 2 * i / turns;
            double radius = maxRadius * (i / (double) turns);
            Location loc = center.clone().add(Math.cos(angle) * radius, i * 0.3, Math.sin(angle) * radius);
            world.spawnParticle(particle, loc, 2, 0, 0, 0, verticalSpeed);
        }
    }

    public void playAbilityImpactVisuals(Location location, AspectType aspect, double intensity, Collection<LivingEntity> targets) {
        if (!enabled) return;
        
        AspectVisualThemes.Theme theme = themes.getTheme(aspect);
        
        // Impact particles
        particles.impact(null, theme, location, (int)intensity);
        
        // Sound
        soundscapes.playImpactSound(null, theme, location, (int)intensity, intensity > 1.5);
        
        // Screen shake for nearby players
        if (screenShakeEnabled) {
            for (Player player : location.getWorld().getPlayers()) {
                double dist = player.getLocation().distance(location);
                if (dist <= 20) {
                    double shakeIntensity = intensity * (1.0 - dist / 20.0) * 0.8;
                    screenEffects.explosionScreenShake(player, location, shakeIntensity, (int)(10 * shakeIntensity));
                }
            }
        }
        
        // Target effects
        if (targets != null) {
            for (LivingEntity target : targets) {
                if (target instanceof Player player) {
                    screenEffects.hitFlash(player, theme);
                    if (screenShakeEnabled) {
                        screenEffects.screenShake(player, 0.5, 8);
                    }
                    soundscapes.playHitSound(player, theme, intensity > 1.5);
                }
                particles.impactDirectional(null, theme, target.getLocation().add(0, 1, 0), 
                    target.getLocation().toVector().subtract(location.toVector()).normalize(), (int)intensity);
            }
        }
    }

    public void playAspectShiftVisuals(Player player, AspectType oldAspect, AspectType newAspect) {
        if (!enabled) return;
        
        AspectVisualThemes.Theme oldTheme = themes.getTheme(oldAspect);
        AspectVisualThemes.Theme newTheme = themes.getTheme(newAspect);
        
        // Dramatic transition
        screenEffects.aspectTransition(player, oldTheme, newTheme);
        soundscapes.playAspectTransitionSound(player, oldTheme, newTheme);
        
        // Custom shift explosion
        Location loc = player.getLocation();
        for (int i = 0; i < 3; i++) {
            final int p = i;
            new BukkitRunnable() {
                @Override
                public void run() {
                    // Old theme particles fading
                    for (int j = 0; j < 20; j++) {
                        double a = Math.random() * Math.PI * 2;
                        double r = 2 + p;
                        loc.getWorld().spawnParticle(oldTheme.getAuraParticle(), 
                            loc.clone().add(Math.cos(a) * r, Math.random() * 2, Math.sin(a) * r), 1, 0, 0, 0, 0);
                    }
                    // New theme particles emerging
                    for (int j = 0; j < 20; j++) {
                        double a = Math.random() * Math.PI * 2;
                        double r = 3 - p;
                        loc.getWorld().spawnParticle(newTheme.getAuraParticle(), 
                            loc.clone().add(Math.cos(a) * r, Math.random() * 2, Math.sin(a) * r), 1, 0, 0, 0, 0);
                    }
                }
            }.runTaskLater(plugin, p * 10L);
        }
    }

    public void playHeartUpgradeVisuals(Player player, int newTier) {
        if (!enabled) return;
        
        AspectVisualThemes.Theme theme = themes.getTheme(
            plugin.getHeartManager().getHeart(player.getUniqueId())
                .map(Heart::getAspect).orElse(AspectType.INFERNO)
        );
        
        screenEffects.tierUpFlash(player, theme, newTier);
        soundscapes.playTierUpSound(player, theme, newTier);
        
        // Tier up particle effect
        Location loc = player.getLocation().add(0, 1, 0);
        for (int i = 0; i < newTier; i++) {
            final int t = i;
            new BukkitRunnable() {
                @Override
                public void run() {
                    // Rising ring
                    for (int j = 0; j < 20; j++) {
                        double a = j * Math.PI * 2 / 20;
                        Location p = loc.clone().add(Math.cos(a) * (t + 1), t * 0.5, Math.sin(a) * (t + 1));
                        p.getWorld().spawnParticle(theme.getAuraParticle(), p, 2, 0.1, 0.1, 0.1, 0.02);
                    }
                    // Vertical beam
                    for (int h = 0; h < 10; h++) {
                        Location p = loc.clone().add(0, h * 0.5, 0);
                        p.getWorld().spawnParticle(theme.getCastParticle(), p, 1, 0, 0, 0, 0);
                    }
                }
            }.runTaskLater(plugin, t * 20L);
        }
    }

    public void playStabilityChangeVisuals(Player player, int oldStability, int newStability) {
        if (!enabled) return;
        
        AspectVisualThemes.Theme theme = themes.getTheme(
            plugin.getHeartManager().getHeart(player.getUniqueId())
                .map(Heart::getAspect).orElse(AspectType.INFERNO)
        );
        
        if (newStability <= 0) {
            screenEffects.heartDeath(player, theme);
            soundscapes.playHeartDeathSound(player, theme);
            particles.spawnHeartDeathEffect(player.getLocation(), theme, particleQuality);
        } else if (newStability < 30 && oldStability >= 30) {
            screenEffects.criticalStabilityWarning(player, theme);
            soundscapes.playStabilityWarning(player, theme);
        } else if (newStability >= 90 && oldStability < 90) {
            screenEffects.awakenedFlash(player, theme);
            soundscapes.playAwakenedSound(player, theme);
            particles.spawnAwakenedAura(player.getLocation(), theme, particleQuality);
        }
    }

    public void playEssenceGainVisuals(Player player, long amount) {
        if (!enabled || amount < 5) return;
        
        AspectVisualThemes.Theme theme = themes.getTheme(
            plugin.getHeartManager().getHeart(player.getUniqueId())
                .map(Heart::getAspect).orElse(AspectType.INFERNO)
        );
        
        particles.spawnEssenceGainParticles(player.getLocation(), theme, (int)(amount / 5), particleQuality);
        soundscapes.playEssenceGainSound(player, theme, amount);
    }

    public void playKillVisuals(Player killer, Player victim) {
        if (!enabled) return;
        
        AspectVisualThemes.Theme theme = themes.getTheme(
            plugin.getHeartManager().getHeart(killer.getUniqueId())
                .map(Heart::getAspect).orElse(AspectType.INFERNO)
        );
        
        screenEffects.killFlash(killer, theme);
        soundscapes.playKillSound(killer, theme);
        particles.spawnKillEffect(victim.getLocation(), theme, particleQuality);
    }

    public void playDeathVisuals(Player victim) {
        if (!enabled) return;
        
        AspectVisualThemes.Theme theme = themes.getTheme(
            plugin.getHeartManager().getHeart(victim.getUniqueId())
                .map(Heart::getAspect).orElse(AspectType.INFERNO)
        );
        
        screenEffects.deathScreen(victim, theme);
        soundscapes.playDeathSound(victim, theme);
        particles.spawnDeathEffect(victim.getLocation(), theme, particleQuality);
    }

    // ════════════════════════════════════════════════════════════════════
    // EVENT VISUALS
    // ════════════════════════════════════════════════════════════════════

    public void playEventPhaseTransition(Player player, EventPhase phase, AspectType eventAspect) {
        if (!enabled) return;
        
        AspectVisualThemes.Theme theme = themes.getTheme(eventAspect);
        screenEffects.eventPhaseTransition(player, theme, phase);
        soundscapes.playEventPhaseSound(player, theme, phase);
        
        // Phase transition particles
        Location loc = player.getLocation();
        particles.spawnEventPhaseEffect(loc, theme, phase, particleQuality);
    }

    public void playGlobalEventAnnouncement(String title, String subtitle, AspectType eventAspect) {
        if (!enabled) return;
        
        AspectVisualThemes.Theme theme = themes.getTheme(eventAspect);
        Color color = theme.getPrimaryColor();
        
        for (Player player : Bukkit.getOnlinePlayers()) {
            screenEffects.globalAnnouncement(player, title, subtitle, color);
            soundscapes.playGlobalAnnouncementSound(player, theme);
        }
        
        particles.spawnGlobalAnnouncementEffect(theme, particleQuality);
    }

    // ════════════════════════════════════════════════════════════════════
    // GETTERS
    // ════════════════════════════════════════════════════════════════════

    public AspectVisualThemes getThemes() { return themes; }
    public ScreenEffectsAPI getScreenEffects() { return screenEffects; }
    public ParticleEffects getParticles() { return particles; }
    public DynamicSoundscapes getSoundscapes() { return soundscapes; }
    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
    public int getParticleQuality() { return particleQuality; }
    public void setParticleQuality(int quality) { this.particleQuality = Math.max(1, Math.min(4, quality)); }
    public boolean isScreenShakeEnabled() { return screenShakeEnabled; }
    public void setScreenShakeEnabled(boolean enabled) { this.screenShakeEnabled = enabled; }
    public boolean isVignetteEnabled() { return vignetteEnabled; }
    public void setVignetteEnabled(boolean enabled) { this.vignetteEnabled = enabled; }
    public boolean isChromaticAberrationEnabled() { return chromaticAberrationEnabled; }
    public void setChromaticAberrationEnabled(boolean enabled) { this.chromaticAberrationEnabled = enabled; }

    public void shutdown() {
        enabled = false;
        passiveEffectTasks.values().forEach(BukkitTask::cancel);
        passiveEffectTasks.clear();
        ambientTrailTasks.values().forEach(BukkitTask::cancel);
        ambientTrailTasks.clear();
        orbitEffectTasks.values().forEach(BukkitTask::cancel);
        orbitEffectTasks.clear();
        trailEffectTasks.values().forEach(BukkitTask::cancel);
        trailEffectTasks.clear();
        playerEffectState.clear();
        particles.shutdown();
        soundscapes.shutdown();
    }
}