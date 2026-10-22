package com.aspectsmp.event;

import com.aspectsmp.AspectSMP;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.title.Title;
import net.kyori.adventure.audience.Audience;
import org.bukkit.*;
import org.bukkit.entity.*;
import org.bukkit.Sound;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class EventEffects {

    private final AspectSMP plugin;
    private final Map<UUID, BossBar> playerBossBars = new ConcurrentHashMap<>();
    private final Map<UUID, Location> lastPlayerLocations = new ConcurrentHashMap<>();

    public EventEffects(AspectSMP plugin) {
        this.plugin = plugin;
    }

    // ══════════════════════════════════════════════════════════════
    // CINEMATIC TITLE/SCREEN EFFECTS
    // ══════════════════════════════════════════════════════════════

    public void sendCinematicTitle(Player player, String title, String subtitle, int fadeIn, int stay, int fadeOut) {
        Title.Times times = Title.Times.times(
            java.time.Duration.ofMillis(fadeIn * 50L),
            java.time.Duration.ofMillis(stay * 50L),
            java.time.Duration.ofMillis(fadeOut * 50L)
        );
        player.showTitle(Title.title(
            Component.text(title).color(NamedTextColor.WHITE).decorate(TextDecoration.BOLD),
            Component.text(subtitle).color(NamedTextColor.GRAY),
            times
        ));
    }

    public void sendActionBarAll(String message) {
        for (Player p : Bukkit.getOnlinePlayers()) {
            p.sendActionBar(Component.text(message).color(NamedTextColor.WHITE));
        }
    }

    public void sendBossBar(Player player, String id, String name, float progress, BossBar.Color color, BossBar.Overlay overlay) {
        BossBar bar = BossBar.bossBar(
            Component.text(name).color(NamedTextColor.WHITE).decorate(TextDecoration.BOLD),
            progress,
            color,
            overlay
        );
        player.showBossBar(bar);
        playerBossBars.put(UUID.fromString(id), bar);
    }

    public void updateBossBar(Player player, String id, float progress, String name) {
        BossBar bar = playerBossBars.get(UUID.fromString(id));
        if (bar != null) {
            bar.progress(progress);
            if (name != null) bar.name(Component.text(name).color(NamedTextColor.WHITE).decorate(TextDecoration.BOLD));
        }
    }

    public void removeBossBar(Player player, String id) {
        BossBar bar = playerBossBars.remove(UUID.fromString(id));
        if (bar != null) player.hideBossBar(bar);
    }

    // ══════════════════════════════════════════════════════════════
    // SCREEN SHAKE / VISION EFFECTS
    // ══════════════════════════════════════════════════════════════

    public void screenShake(Player player, double intensity, int durationTicks) {
        new BukkitRunnable() {
            int ticks = 0;
            @Override
            public void run() {
                if (!player.isOnline() || ticks >= durationTicks) { cancel(); return; }
                Vector shake = new Vector(
                    (Math.random() - 0.5) * intensity,
                    (Math.random() - 0.5) * intensity * 0.3,
                    (Math.random() - 0.5) * intensity
                );
                player.setVelocity(player.getVelocity().add(shake));
                ticks++;
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }

    public void visionDistortion(Player player, PotionEffectType type, int amplifier, int durationTicks) {
        player.addPotionEffect(new PotionEffect(type, durationTicks, amplifier, true, false, true));
    }

    public void nauseaFlash(Player player, int durationTicks) {
        player.addPotionEffect(new PotionEffect(PotionEffectType.NAUSEA, durationTicks, 1, true, false));
        player.addPotionEffect(new PotionEffect(PotionEffectType.NAUSEA, durationTicks, 2, true, false));
    }

    public void blindnessPulse(Player player, int durationTicks, int amplifier) {
        player.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, durationTicks, amplifier, true, false));
    }

    // ══════════════════════════════════════════════════════════════
    // SOUNDSCAPE - LAYERED, DYNAMIC, DIRECTIONAL
    // ══════════════════════════════════════════════════════════════

    public void playGlobalSoundscape(World world, Location center, int radius, org.bukkit.Sound baseSound, float volume, float pitch, int layers) {
        for (Player p : world.getPlayers()) {
            if (p.getLocation().distance(center) <= radius) {
                double dist = p.getLocation().distance(center);
                float vol = volume * (1.0f - (float)(dist / radius) * 0.5f);
                
                Location loc = p.getLocation();
                // Base sound
                p.playSound(loc, baseSound, vol, pitch);
                
                // Layered echoes
                for (int i = 1; i <= layers; i++) {
                    float layerPitch = pitch + (i * 0.15f) - 0.1f;
                    float layerVol = vol * (0.4f / i);
                    new BukkitRunnable() {
                        @Override
                        public void run() {
                            if (p.isOnline()) {
                                p.playSound(p.getLocation(), baseSound, layerVol, layerPitch);
                            }
                        }
                    }.runTaskLater(plugin, i * 4L);
                }
            }
        }
    }

    public void playDirectionalSound(Player player, Location source, org.bukkit.Sound sound, float volume, float pitch) {
        Vector toSource = source.toVector().subtract(player.getLocation().toVector());
        double dist = toSource.length();
        float attenuatedVol = (float) (volume / (1.0 + dist * 0.02));
        player.playSound(player.getLocation(), sound, Math.max(0.1f, attenuatedVol), pitch);
    }

    public void playHeartbeat(Player player, int bpm, int durationTicks) {
        new BukkitRunnable() {
            int ticks = 0;
            int beatInterval = 1200 / bpm; // ticks per beat
            @Override
            public void run() {
                if (!player.isOnline() || ticks >= durationTicks) { cancel(); return; }
                if (ticks % beatInterval == 0) {
                    player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_PLAYER_HEARTBEAT, 1.0f, 1.0f);
                }
                ticks++;
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }

    // ══════════════════════════════════════════════════════════════
    // PARTICLE SYSTEMS - COMPLEX, LAYERED, PHYSICS-BASED
    // ══════════════════════════════════════════════════════════════

    public void spawnParticleSphere(Location center, Particle particle, int rings, int particlesPerRing, double radius, double speed) {
        for (int ring = 0; ring < rings; ring++) {
            double y = -radius + (radius * 2.0 * ring / rings);
            double ringRadius = Math.sqrt(radius * radius - y * y);
            for (int i = 0; i < particlesPerRing; i++) {
                double angle = 2 * Math.PI * i / particlesPerRing;
                double x = Math.cos(angle) * ringRadius;
                double z = Math.sin(angle) * ringRadius;
                center.getWorld().spawnParticle(particle, center.clone().add(x, y, z), 1, 0, 0, 0, speed);
            }
        }
    }

    public void spawnParticleTornado(Location base, Particle particle, int height, int particlesPerLevel, double baseRadius, double topRadius, int durationTicks) {
        new BukkitRunnable() {
            int ticks = 0;
            @Override
            public void run() {
                if (ticks >= durationTicks) { cancel(); return; }
                double progress = (double) ticks / durationTicks;
                for (int level = 0; level < height; level++) {
                    double yProgress = (double) level / height;
                    double radius = baseRadius + (topRadius - baseRadius) * yProgress;
                    double y = base.getY() + level;
                    for (int i = 0; i < particlesPerLevel; i++) {
                        double angle = 2 * Math.PI * i / particlesPerLevel + ticks * 0.3;
                        double x = Math.cos(angle) * radius;
                        double z = Math.sin(angle) * radius;
                        base.getWorld().spawnParticle(particle, base.clone().add(x, y, z), 1, 0, 0, 0, 0.01);
                    }
                }
                ticks++;
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }

    public void spawnParticleWave(Location center, Particle particle, double maxRadius, int rings, int particlesPerRing, int durationTicks, double speed) {
        new BukkitRunnable() {
            int ticks = 0;
            @Override
            public void run() {
                if (ticks >= durationTicks) { cancel(); return; }
                double progress = (double) ticks / durationTicks;
                double currentRadius = maxRadius * progress;
                for (int ring = 0; ring < rings; ring++) {
                    double r = currentRadius * (0.3 + 0.7 * ring / (double) rings);
                    for (int i = 0; i < particlesPerRing; i++) {
                        double angle = 2 * Math.PI * i / particlesPerRing + ticks * 0.1;
                        double x = Math.cos(angle) * r;
                        double z = Math.sin(angle) * r;
                        center.getWorld().spawnParticle(particle, center.clone().add(x, 0.5, z), 1, 0, 0, 0, 0.02);
                    }
                }
                ticks++;
            }
        }.runTaskTimer(plugin, 0L, 2L);
    }

    public void spawnAscendingParticles(Location base, Particle particle, int count, double height, double spread, int durationTicks) {
        new BukkitRunnable() {
            int ticks = 0;
            @Override
            public void run() {
                if (ticks >= durationTicks) { cancel(); return; }
                for (int i = 0; i < count; i++) {
                    double progress = (ticks + Math.random()) / durationTicks;
                    double y = base.getY() + progress * height;
                    double x = (Math.random() - 0.5) * spread * (1.0 - progress * 0.5);
                    double z = (Math.random() - 0.5) * spread * (1.0 - progress * 0.5);
                    base.getWorld().spawnParticle(particle, base.clone().add(x, y, z), 1, 0, 0, 0, 0.01);
                }
                ticks++;
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }

    public void spawnDescendingParticles(Location base, Particle particle, int count, double height, double spread, int durationTicks) {
        new BukkitRunnable() {
            int ticks = 0;
            @Override
            public void run() {
                if (ticks >= durationTicks) { cancel(); return; }
                for (int i = 0; i < count; i++) {
                    double progress = (ticks + Math.random()) / durationTicks;
                    double y = base.getY() + height - progress * height;
                    double x = (Math.random() - 0.5) * spread * progress;
                    double z = (Math.random() - 0.5) * spread * progress;
                    base.getWorld().spawnParticle(particle, base.clone().add(x, y, z), 1, 0, 0, 0, 0.02);
                }
                ticks++;
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }

    // ══════════════════════════════════════════════════════════════
    // WORLD MODIFICATION - STRUCTURES, BLOCKS, ENVIRONMENT
    // ══════════════════════════════════════════════════════════════

    public void createRiftStructure(Location center, int radius, int height, Material primary, Material secondary) {
        // Central pillar
        for (int y = 0; y < height; y++) {
            center.clone().add(0, y, 0).getBlock().setType(y == height - 1 ? secondary : primary);
        }
        // Rings
        for (int r = 1; r <= radius; r++) {
            for (int y = 0; y < Math.max(1, height - r); y++) {
                for (int angle = 0; angle < 360; angle += 15) {
                    double rad = Math.toRadians(angle);
                    int x = (int) Math.round(Math.cos(rad) * r);
                    int z = (int) Math.round(Math.sin(rad) * r);
                    center.clone().add(x, y, z).getBlock().setType(primary);
                }
            }
        }
        // Floating debris
        for (int i = 0; i < 20; i++) {
            double angle = Math.random() * 2 * Math.PI;
            double dist = radius * 0.5 + Math.random() * radius * 0.5;
            double y = Math.random() * height * 1.5;
            Location debris = center.clone().add(Math.cos(angle) * dist, y, Math.sin(angle) * dist);
            debris.getBlock().setType(Material.END_STONE);
        }
    }

    public void createMonolith(Location center, int height, Material core, Material shell) {
        for (int y = 0; y < height; y++) {
            Location l = center.clone().add(0, y, 0);
            l.getBlock().setType(y == height - 1 ? core : shell);
            // 4 pillars
            for (int dir = 0; dir < 4; dir++) {
                double angle = dir * Math.PI / 2;
                Location p = l.clone().add(Math.cos(angle) * 2, 0, Math.sin(angle) * 2);
                p.getBlock().setType(shell);
            }
        }
        // Glowing top
        center.clone().add(0, height, 0).getBlock().setType(Material.BEACON);
    }

    public void spreadCorruption(Location center, int radius, Material to, Material... from) {
        Set<Material> fromSet = new HashSet<>(Arrays.asList(from));
        for (int x = -radius; x <= radius; x++) {
            for (int z = -radius; z <= radius; z++) {
                if (x*x + z*z <= radius*radius) {
                    for (int y = -3; y <= 3; y++) {
                        Location l = center.clone().add(x, y, z);
                        if (fromSet.contains(l.getBlock().getType())) {
                            if (Math.random() < 0.15) l.getBlock().setType(to);
                        }
                    }
                }
            }
        }
    }

    public void createLightningWeb(Location center, int radius, int strikes) {
        new BukkitRunnable() {
            int count = 0;
            @Override
            public void run() {
                if (count >= strikes) { cancel(); return; }
                Location strike = center.clone().add(
                    (Math.random() - 0.5) * radius * 2,
                    0,
                    (Math.random() - 0.5) * radius * 2
                );
                strike.setY(strike.getWorld().getHighestBlockYAt(strike) + 5);
                strike.getWorld().strikeLightningEffect(strike);
                strike.getWorld().playSound(strike, org.bukkit.Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 2.0f, 0.5f);
                count++;
            }
        }.runTaskTimer(plugin, 0L, 10L);
    }

    // ══════════════════════════════════════════════════════════════
    // ENTITY EFFECTS - TRAILS, AURAS, CUSTOM AI HINTS
    // ══════════════════════════════════════════════════════════════

    public void addEntityTrail(LivingEntity entity, Particle particle, int durationTicks, int particlesPerTick) {
        new BukkitRunnable() {
            int ticks = 0;
            @Override
            public void run() {
                if (!entity.isValid() || entity.isDead() || ticks >= durationTicks) { cancel(); return; }
                for (int i = 0; i < particlesPerTick; i++) {
                    Location loc = entity.getLocation().add(
                        (Math.random() - 0.5) * entity.getWidth(),
                        Math.random() * entity.getHeight(),
                        (Math.random() - 0.5) * entity.getWidth()
                    );
                    loc.getWorld().spawnParticle(particle, loc, 1, 0, 0, 0, 0.01);
                }
                ticks++;
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }

    public void addEntityAura(LivingEntity entity, Particle inner, Particle outer, int innerRadius, int outerRadius, int durationTicks) {
        new BukkitRunnable() {
            int ticks = 0;
            @Override
            public void run() {
                if (!entity.isValid() || entity.isDead() || ticks >= durationTicks) { cancel(); return; }
                Location loc = entity.getLocation().add(0, entity.getHeight() / 2.0, 0);
                entity.getWorld().spawnParticle(inner, loc, 10, innerRadius, innerRadius, innerRadius, 0.01);
                entity.getWorld().spawnParticle(outer, loc, 5, outerRadius, outerRadius, outerRadius, 0.01);
                ticks++;
            }
        }.runTaskTimer(plugin, 0L, 2L);
    }

    public void makeEntityPhase(LivingEntity entity, int durationTicks) {
        new BukkitRunnable() {
            int ticks = 0;
            @Override
            public void run() {
                if (!entity.isValid() || entity.isDead() || ticks >= durationTicks) { cancel(); return; }
                entity.addPotionEffect(new PotionEffect(PotionEffectType.INVISIBILITY, 2, 0, true, false), true);
                entity.getWorld().spawnParticle(Particle.PORTAL, entity.getLocation().add(0, 1, 0), 5, 0.3, 0.5, 0.3, 0.02);
                ticks++;
            }
        }.runTaskTimer(plugin, 0L, 2L);
    }

    // ══════════════════════════════════════════════════════════════
    // PHASE TRANSITION CINEMATICS
    // ══════════════════════════════════════════════════════════════

    public void phaseTransitionCinematic(Player player, String phaseName, String description, Color themeColor) {
        // Pre-flash
        player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_ENDER_DRAGON_GROWL, 1.0f, 0.3f);
        visionDistortion(player, PotionEffectType.BLINDNESS, 10, 40);
        
        new BukkitRunnable() {
            @Override
            public void run() {
                if (!player.isOnline()) return;
                sendCinematicTitle(player, phaseName, description, 20, 100, 20);
                player.playSound(player.getLocation(), org.bukkit.Sound.UI_TOAST_CHALLENGE_COMPLETE, 1.0f, 1.2f);
                screenShake(player, 1.5, 20);
                
                // Color flash
                player.getWorld().spawnParticle(Particle.EXPLOSION_EMITTER, player.getLocation().add(0, 2, 0), 20, 2, 2, 2, 0.1);
            }
        }.runTaskLater(plugin, 20L);
    }

    public void globalPhaseAnnouncement(String phaseName, String description, org.bukkit.Sound sound, float volume, float pitch) {
        Component title = Component.text(phaseName).color(NamedTextColor.WHITE).decorate(TextDecoration.BOLD);
        Component desc = Component.text(description).color(NamedTextColor.GRAY);
        
        for (Player p : Bukkit.getOnlinePlayers()) {
            p.showTitle(Title.title(title, desc, Title.Times.times(
                java.time.Duration.ofSeconds(2),
                java.time.Duration.ofSeconds(5),
                java.time.Duration.ofSeconds(2)
            )));
            p.playSound(p.getLocation(), sound, volume, pitch);
        }
    }

    // ══════════════════════════════════════════════════════════════
    // UTILITY
    // ══════════════════════════════════════════════════════════════

    public Color getPhaseColor(EventPhase phase) {
        return switch (phase) {
            case SKY_AWAKENS, SKY_FRAGMENTS, SKY_RIFTS_OPEN -> Color.fromRGB(0x87CEEB); // Sky blue
            case SKY_STORMS, SKY_GUARDIAN -> Color.fromRGB(0x4682B4); // Steel blue
            case SKY_CONVERGENCE, SKY_COMPLETED -> Color.fromRGB(0x00BFFF); // Deep sky blue
            
            case FIRST_FROST, DEEP_FREEZE -> Color.fromRGB(0xADD8E6); // Light blue
            case FROZEN_TITANS, FROST_MONARCH -> Color.fromRGB(0x5D8AA8); // Air force blue
            case FROZEN_CRUCIBLE, WINTER_COMPLETED -> Color.fromRGB(0x00FFFF); // Cyan
            
            case SHADOW_AWAKENS, SHADOW_FRAGMENTS -> Color.fromRGB(0x2F2F4F); // Dark slate
            case SHADOW_RIFTS, SHADOW_STALKERS -> Color.fromRGB(0x1A1A2E); // Very dark blue
            case SHADOW_LORD, SHADOW_CONVERGENCE, SHADOW_COMPLETED -> Color.fromRGB(0x0D0D1A); // Near black
            
            case STORM_AWAKENS, STORM_FRAGMENTS -> Color.fromRGB(0xB0C4DE); // Light steel blue
            case STORM_GUSTS, STORM_CYCLONES -> Color.fromRGB(0x778899); // Light slate gray
            case STORM_TITAN, STORM_CONVERGENCE, STORM_COMPLETED -> Color.fromRGB(0x4682B4); // Steel blue
            
            default -> Color.WHITE;
        };
    }
}