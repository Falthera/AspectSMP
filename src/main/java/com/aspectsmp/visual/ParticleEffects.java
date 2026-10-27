package com.aspectsmp.visual;

import com.aspectsmp.AspectSMP;
import com.aspectsmp.core.Heart;
import com.aspectsmp.event.EventPhase;
import com.aspectsmp.visual.AspectVisualThemes.Theme;
import org.bukkit.*;
import org.bukkit.entity.*;
import org.bukkit.Particle;
import org.bukkit.Particle.DustOptions;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Vector;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

public class ParticleEffects {

    private final AspectSMP plugin;
    private final Map<UUID, List<BukkitTask>> activeAnimations = new ConcurrentHashMap<>();
    private final boolean useTrailParticles;
    
    // Performance optimization settings
    private final int maxParticlesPerTick;
    private final int maxParticlesPerEffect;
    private final double maxViewDistance;
    private final AtomicInteger particlesThisTick = new AtomicInteger(0);
    private long lastTickReset = System.currentTimeMillis();
    private int tickResetTaskId = -1;

    public ParticleEffects(AspectSMP plugin) {
        this.plugin = plugin;
        this.useTrailParticles = plugin.getConfig().getBoolean("visual-effects.use-trail-particles", true);
        
        // Load performance settings from config
        this.maxParticlesPerTick = plugin.getConfig().getInt("visual-effects.max-particles-per-tick", 200);
        this.maxParticlesPerEffect = plugin.getConfig().getInt("visual-effects.max-particles-per-effect", 50);
        this.maxViewDistance = plugin.getConfig().getDouble("visual-effects.max-view-distance", 48.0);
        
        // Start tick reset task
        tickResetTaskId = plugin.getServer().getScheduler().runTaskTimer(plugin, () -> {
            long now = System.currentTimeMillis();
            if (now - lastTickReset >= 50) {
                particlesThisTick.set(0);
                lastTickReset = now;
            }
        }, 1L, 1L).getTaskId();
    }

    private boolean canSpawnParticles(int count) {
        return particlesThisTick.addAndGet(count) <= maxParticlesPerTick;
    }

    private int clampCount(int count) {
        return Math.min(count, maxParticlesPerEffect);
    }

    private boolean isInViewRange(Location loc, Player viewer) {
        return loc.distanceSquared(viewer.getLocation()) <= maxViewDistance * maxViewDistance;
    }

    private boolean shouldShowToPlayer(Location loc, Player player) {
        // Only show particles to nearby players to reduce client load
        return player.isOnline() && !player.isDead() && isInViewRange(loc, player);
    }

    // ═════════════════════════════════════════════════════════════════════
    // CORE PARTICLE HELPERS - 1.21.11 Particle.Builder API
    // ════════════════════════════════════════════════════════════════════

    private Particle.DustOptions dust(Color color, float size) {
        return new Particle.DustOptions(color, size);
    }

    private Particle.DustOptions dust(Color color) {
        return dust(color, 1.0f);
    }

    private void spawnParticle(World world, Particle particle, Location location, int count) {
        if (count <= 0) return;
        count = clampCount(count);
        if (!canSpawnParticles(count)) return;
        particle.builder().location(location).count(count).spawn();
    }

    private void spawnParticle(World world, Particle particle, Location location, int count, double offsetX, double offsetY, double offsetZ, double extra) {
        if (count <= 0) return;
        count = clampCount(count);
        if (!canSpawnParticles(count)) return;
        particle.builder()
            .location(location)
            .count(count)
            .offset(offsetX, offsetY, offsetZ)
            .extra(extra)
            .spawn();
    }

    private void spawnParticle(World world, Particle particle, Location location, int count, double offsetX, double offsetY, double offsetZ, double extra, Particle.DustOptions dust) {
        if (count <= 0) return;
        count = clampCount(count);
        if (!canSpawnParticles(count)) return;
        particle.builder()
            .location(location)
            .count(count)
            .offset(offsetX, offsetY, offsetZ)
            .extra(extra)
            .data(dust)
            .spawn();
    }

    private void spawnTrailParticle(World world, Location from, Location to, Color color, int duration) {
        if (!useTrailParticles) return;
        world.spawnParticle(Particle.TRAIL, from, 1, 0, 0, 0, 0, new Particle.Trail(to, color, duration), true);
    }

    // ═════════════════════════════════════════════════════════════════════
    // AURA EFFECTS - Persistent around player (OPTIMIZED)
    // ═════════════════════════════════════════════════════════════════════

    public void startPassiveAura(Player player, Theme theme, int stability) {
        UUID id = player.getUniqueId();
        stopPassiveAura(player);

        BukkitTask task = new BukkitRunnable() {
            double angle = 0;
            double height = 0;
            int tick = 0;
            @Override
            public void run() {
                if (!player.isOnline() || player.isDead()) { cancel(); return; }

                Location loc = player.getLocation().add(0, 1, 0);
                Theme t = theme;
                Color primary = t.getPrimaryColor();
                Color secondary = t.getSecondaryColor();
                Color accent = t.getAccentColor();
                Particle auraParticle = t.getAuraParticle();

                // Fetch current stability each tick
                int currentStability = plugin.getHeartManager().getHeart(player.getUniqueId())
                    .map(Heart::getStability).orElse(100);

                // OPTIMIZED: Reduce particle count based on distance/view
                int maxAuraParticles = Math.min(t.getAuraParticleCount(), maxParticlesPerEffect / 2);
                
                // Base aura ring - spawn fewer particles per tick, spread over time
                int particlesThisTick = Math.min(maxAuraParticles / 4 + 1, maxAuraParticles);
                for (int i = 0; i < particlesThisTick; i++) {
                    int idx = (tick + i) % maxAuraParticles;
                    double a = angle + (idx * Math.PI * 2 / maxAuraParticles);
                    double radius = 1.2 + Math.sin(tick * 0.1 + idx) * 0.3;
                    Vector v = new Vector(Math.cos(a) * radius, 0, Math.sin(a) * radius);
                    loc.add(v);
                    spawnParticle(loc.getWorld(), auraParticle, loc, 1);
                    loc.subtract(v);
                }

                // Stability-based secondary ring (only when low)
                if (currentStability < 50 && tick % 2 == 0) {
                    for (int i = 0; i < 2; i++) {
                        double a = angle + Math.PI / 2 + (i * Math.PI);
                        double radius = 0.8;
                        Vector v = new Vector(Math.cos(a) * radius, height, Math.sin(a) * radius);
                        loc.add(v);
                        spawnParticle(loc.getWorld(), t.getOrbitParticle(), loc, 1);
                        loc.subtract(v);
                    }
                }

                // Critical stability - inner chaos ring (less frequent)
                if (currentStability < 20 && tick % 3 == 0) {
                    for (int i = 0; i < 3; i++) {
                        double a = -angle * 1.5 + (i * Math.PI * 2 / 3);
                        double radius = 0.5 + Math.random() * 0.3;
                        Vector v = new Vector(Math.cos(a) * radius, (Math.random() - 0.5) * 1.5, Math.sin(a) * radius);
                        loc.add(v);
                        spawnParticle(loc.getWorld(), Particle.SOUL_FIRE_FLAME, loc, 1);
                        loc.subtract(v);
                    }
                }

                // Awakened - golden particles rising (every 2 ticks)
                if (currentStability >= 100 && tick % 2 == 0) {
                    for (int i = 0; i < 2; i++) {
                        double a = angle + (i * Math.PI);
                        double radius = 1.5;
                        Vector v = new Vector(Math.cos(a) * radius, height, Math.sin(a) * radius);
                        loc.add(v);
                        spawnParticle(loc.getWorld(), Particle.END_ROD, loc, 1);
                        loc.subtract(v);
                    }
                }

                angle += t.getAuraSpeed();
                height = Math.sin(tick * 0.05) * 0.5;
                tick++;
            }
        }.runTaskTimer(plugin, 0L, 1L);

        activeAnimations.computeIfAbsent(id, k -> new ArrayList<>()).add(task);
    }

    public void stopPassiveAura(Player player) {
        List<BukkitTask> tasks = activeAnimations.remove(player.getUniqueId());
        if (tasks != null) tasks.forEach(BukkitTask::cancel);
    }

    // ═════════════════════════════════════════════════════════════════════
    // ORBIT EFFECTS - Particles orbiting player (OPTIMIZED)
    // ═════════════════════════════════════════════════════════════════════

    public void startOrbitEffect(Player player, Theme theme, int tier) {
        UUID id = player.getUniqueId();
        
        BukkitTask task = new BukkitRunnable() {
            double angle = 0;
            double verticalAngle = 0;
            @Override
            public void run() {
                if (!player.isOnline() || player.isDead()) { cancel(); return; }

                Location loc = player.getLocation().add(0, 1.5, 0);
                // Reduced particle count, spread over multiple ticks
                int maxParticles = 4 + tier;
                int particlesPerTick = Math.max(1, maxParticles / 2);
                
                for (int i = 0; i < particlesPerTick; i++) {
                    int idx = (tick + i) % maxParticles;
                    double a = angle + (idx * Math.PI * 2 / maxParticles);
                    double radius = 1.0 + tier * 0.3;
                    double y = Math.sin(verticalAngle + idx) * 0.5;
                    
                    Vector v = new Vector(Math.cos(a) * radius, y, Math.sin(a) * radius);
                    loc.add(v);
                    spawnParticle(loc.getWorld(), theme.getOrbitParticle(), loc, 1);
                    loc.subtract(v);
                }

                angle += theme.getOrbitSpeed();
                verticalAngle += 0.02;
                tick++;
            }
            
            int tick = 0;
        }.runTaskTimer(plugin, 0L, 1L);

        activeAnimations.computeIfAbsent(id, k -> new ArrayList<>()).add(task);
    }

    // ═════════════════════════════════════════════════════════════════════
    // TRAIL EFFECTS - Behind moving player (OPTIMIZED)
    // ═════════════════════════════════════════════════════════════════════

    public void startTrailEffect(Player player, Theme theme) {
        UUID id = player.getUniqueId();
        Location[] lastLoc = { player.getLocation() };

        BukkitTask task = new BukkitRunnable() {
            @Override
            public void run() {
                if (!player.isOnline() || player.isDead()) { cancel(); return; }

                Location loc = player.getLocation();
                
                // Only spawn if moved
                if (loc.distance(lastLoc[0]) > 0.2) {
                    // Reduced trail particle count
                    int trailCount = Math.min(theme.getTrailParticleCount(), 3);
                    for (int i = 0; i < trailCount; i++) {
                        Vector offset = new Vector(
                            (Math.random() - 0.5) * 0.3,
                            Math.random() * 1.0,
                            (Math.random() - 0.5) * 0.3
                        );
                        loc.add(offset);
                        spawnParticle(loc.getWorld(), theme.getTrailParticle(), loc, 1);
                        loc.subtract(offset);
                    }
                    
                    // 1.21.11 TRAIL particle toward last location (every 3 ticks)
                    if (useTrailParticles && theme.getTrailParticleAlt() != null && tick % 3 == 0) {
                        spawnTrailParticle(loc.getWorld(), loc, lastLoc[0], theme.getPrimaryColor(), 15);
                    }
                    
                    lastLoc[0] = loc.clone();
                }
            }
            
            int tick = 0;
        }.runTaskTimer(plugin, 0L, 2L);

        activeAnimations.computeIfAbsent(id, k -> new ArrayList<>()).add(task);
    }

    public void stopTrailEffect(Player player) {
        // Handled by stopPassiveAura since we use same map
    }

    public void stopOrbitEffect(UUID playerId) {
        List<BukkitTask> tasks = activeAnimations.get(playerId);
        if (tasks != null) {
            for (BukkitTask task : tasks) {
                task.cancel();
            }
            activeAnimations.remove(playerId);
        }
    }

    public void stopTrailEffect(UUID playerId) {
        stopOrbitEffect(playerId);
    }

    // ═════════════════════════════════════════════════════════════════════
    // CAST EFFECTS - Ability charging/casting (OPTIMIZED)
    // ═════════════════════════════════════════════════════════════════════

    public void castCharge(Player player, Theme theme, int tier, int durationTicks) {
        Location loc = player.getLocation().add(0, 1, 0);

        new BukkitRunnable() {
            int tick = 0;
            @Override
            public void run() {
                if (!player.isOnline() || tick >= durationTicks) { cancel(); return; }

                float progress = (float) tick / durationTicks;
                // Reduced particle count
                int maxParticles = Math.min((int) (8 * progress * tier), maxParticlesPerEffect);

                // Converging particles - spread over time
                for (int i = 0; i < Math.min(maxParticles, 8); i++) {
                    double angle = Math.random() * Math.PI * 2;
                    double radius = (1.0 - progress) * 2.0 + 0.3;
                    double y = Math.random() * 2.0;
                    
                    Vector v = new Vector(Math.cos(angle) * radius, y, Math.sin(angle) * radius);
                    Location pLoc = loc.clone().add(v);
                    spawnParticle(pLoc.getWorld(), theme.getCastParticle(), pLoc, 1);
                }

                // Ground ring expanding (every 4 ticks, fewer particles)
                if (tick % 4 == 0) {
                    double ringRadius = progress * 3.0;
                    for (int i = 0; i < 8; i++) {
                        double a = i * Math.PI * 2 / 8;
                        Vector v = new Vector(Math.cos(a) * ringRadius, 0, Math.sin(a) * ringRadius);
                        Location pLoc = loc.clone().add(v);
                        spawnParticle(pLoc.getWorld(), theme.getCastParticle(), pLoc, 1);
                    }
                }

                tick++;
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }

    public void castRelease(Player player, Theme theme, int tier) {
        Location loc = player.getLocation().add(0, 1, 0);

        // Explosive burst - reduced rings and particles
        for (int ring = 0; ring < 2; ring++) {
            final int r = ring;
            new BukkitRunnable() {
                @Override
                public void run() {
                    double radius = 0.5 + r * 0.5;
                    for (int i = 0; i < 12 + tier * 3; i++) {
                        double a = Math.random() * Math.PI * 2;
                        double y = (Math.random() - 0.5) * 1.5;
                        Vector v = new Vector(Math.cos(a) * radius, y, Math.sin(a) * radius);
                        Location pLoc = loc.clone().add(v);
                        Vector velocity = v.clone().normalize().multiply(0.4 + r * 0.15);
                        spawnParticle(pLoc.getWorld(), theme.getCastParticle(), pLoc, 1, 
                            velocity.getX(), velocity.getY(), velocity.getZ(), 0.05);
                    }
                }
            }.runTaskLater(plugin, r * 3L);
        }

        // Vertical beam - reduced
        for (int i = 0; i < 15; i++) {
            double y = i * 0.3;
            Location pLoc = loc.clone().add(0, y, 0);
            spawnParticle(pLoc.getWorld(), theme.getCastParticle(), pLoc, 2, 0.2, 0, 0.2, 0.02);
        }
    }

    // ═════════════════════════════════════════════════════════════════════
    // IMPACT EFFECTS - On hit/landing (OPTIMIZED)
    // ═════════════════════════════════════════════════════════════════════

    public void impact(Player player, Theme theme, Location location, int tier) {
        Color primary = theme.getPrimaryColor();

        // Main impact burst - reduced
        for (int i = 0; i < 10 + tier * 3; i++) {
            Vector v = new Vector(
                (Math.random() - 0.5) * 2,
                Math.random() * 1.5,
                (Math.random() - 0.5) * 2
            ).normalize().multiply(0.1 + Math.random() * 0.3);
            
            spawnParticle(location.getWorld(), theme.getImpactParticle(), location, 1, 
                v.getX(), v.getY(), v.getZ(), 0.1);
        }

        // Ground shockwave - reduced particles
        new BukkitRunnable() {
            double radius = 0;
            @Override
            public void run() {
                if (radius > 3 + tier) { cancel(); return; }
                
                for (int i = 0; i < 12; i++) {
                    double a = i * Math.PI * 2 / 12;
                    Vector v = new Vector(Math.cos(a) * radius, 0.1, Math.sin(a) * radius);
                    Location pLoc = location.clone().add(v);
                    spawnParticle(pLoc.getWorld(), theme.getImpactParticle(), pLoc, 1);
                }
                radius += 0.6;
            }
        }.runTaskTimer(plugin, 0L, 1L);

        // Vertical pillar - reduced
        for (int i = 0; i < 12; i++) {
            double y = i * 0.25;
            Location pLoc = location.clone().add(0, y, 0);
            spawnParticle(pLoc.getWorld(), theme.getImpactParticle(), pLoc, 1, 0.15, 0, 0.15, 0.02);
        }
    }

    public void impactDirectional(Player player, Theme theme, Location location, Vector direction, int tier) {
        // Cone-shaped impact in direction of attack - reduced
        for (int i = 0; i < 12 + tier * 3; i++) {
            double spread = 0.5;
            double angle = Math.atan2(direction.getZ(), direction.getX()) + (Math.random() - 0.5) * spread;
            double y = Math.random() * 0.4;
            double speed = 0.2 + Math.random() * 0.4;
            
            Vector v = new Vector(Math.cos(angle) * speed, y, Math.sin(angle) * speed);
            spawnParticle(location.getWorld(), theme.getImpactParticle(), location, 1, 
                v.getX(), v.getY(), v.getZ(), 0.1);
        }
    }

    // ═════════════════════════════════════════════════════════════════════
    // ULTIMATE EFFECTS - Major abilities (OPTIMIZED)
    // ═════════════════════════════════════════════════════════════════════

    public void ultimate(Player player, Theme theme) {
        Location loc = player.getLocation().add(0, 1, 0);

        // Stage 1: Implosion - reduced particles
        new BukkitRunnable() {
            double radius = 4;
            @Override
            public void run() {
                if (radius < 0.5) { cancel(); return; }
                
                for (int i = 0; i < 15; i++) {
                    double a = Math.random() * Math.PI * 2;
                    double y = (Math.random() - 0.5) * 1.5;
                    Vector v = new Vector(Math.cos(a) * radius, y, Math.sin(a) * radius);
                    Location pLoc = loc.clone().add(v);
                    Vector toCenter = loc.toVector().subtract(pLoc.toVector()).normalize().multiply(0.25);
                    spawnParticle(pLoc.getWorld(), theme.getUltimateParticle(), pLoc, 1,
                        toCenter.getX(), toCenter.getY(), toCenter.getZ(), 0.04);
                }
                radius -= 0.25;
            }
        }.runTaskTimer(plugin, 0L, 1L);

        // Stage 2: Explosion (delayed)
        new BukkitRunnable() {
            @Override
            public void run() {
                // Massive sphere - reduced layers and particles
                for (int layer = 0; layer < 3; layer++) {
                    double r = 1 + layer * 1.0;
                    for (int i = 0; i < 20; i++) {
                        double a = Math.random() * Math.PI * 2;
                        double y = (Math.random() - 0.5) * 1.5;
                        Vector v = new Vector(Math.cos(a) * r, y, Math.sin(a) * r);
                        Location pLoc = loc.clone().add(v);
                        Vector vel = v.clone().normalize().multiply(0.4 + layer * 0.08);
                        spawnParticle(pLoc.getWorld(), theme.getUltimateParticle(), pLoc, 1,
                            vel.getX(), vel.getY(), vel.getZ(), 0.08);
                    }
                }

                // Rising pillar
                new BukkitRunnable() {
                    int height = 0;
                    @Override
                    public void run() {
                        if (height > 30) { cancel(); return; }
                        for (int i = 0; i < 3; i++) {
                            double a = Math.random() * Math.PI * 2;
                            double r = 1.2;
                            Vector v = new Vector(Math.cos(a) * r, height * 0.15, Math.sin(a) * r);
                            Location pLoc = loc.clone().add(v);
                            spawnParticle(pLoc.getWorld(), theme.getUltimateParticle(), pLoc, 1, 0, 0.08, 0, 0);
                        }
                        height++;
                    }
                }.runTaskTimer(plugin, 0L, 1L);

                // Shockwave ring - reduced
                new BukkitRunnable() {
                    double radius = 0;
                    @Override
                    public void run() {
                        if (radius > 15) { cancel(); return; }
                        for (int i = 0; i < 30; i++) {
                            double a = i * Math.PI * 2 / 30;
                            Vector v = new Vector(Math.cos(a) * radius, 0.1, Math.sin(a) * radius);
                            Location pLoc = loc.clone().add(v);
                            spawnParticle(pLoc.getWorld(), Particle.SONIC_BOOM, pLoc, 1);
                        }
                        radius += 0.8;
                    }
                }.runTaskTimer(plugin, 0L, 1L);
            }
        }.runTaskLater(plugin, 20L);
    }

    // ═════════════════════════════════════════════════════════════════════
    // BEAM EFFECTS - Directed abilities (OPTIMIZED)
    // ═════════════════════════════════════════════════════════════════════

    public void beam(Player player, Theme theme, Location from, Location to, double width) {
        Vector direction = to.toVector().subtract(from.toVector());
        double distance = direction.length();
        direction.normalize();

        // Larger step size = fewer particles
        for (double d = 0; d < distance; d += 0.35) {
            Location pLoc = from.clone().add(direction.clone().multiply(d));
            
            // Core beam
            for (int i = 0; i < 2; i++) {
                Vector offset = new Vector(
                    (Math.random() - 0.5) * width * 0.6,
                    (Math.random() - 0.5) * width * 0.6,
                    (Math.random() - 0.5) * width * 0.6
                );
                pLoc.add(offset);
                spawnParticle(pLoc.getWorld(), theme.getCastParticle(), pLoc, 1);
                pLoc.subtract(offset);
            }
            
            // Outer glow (every 0.7 distance)
            if (d % 0.7 < 0.3) {
                for (int i = 0; i < 4; i++) {
                    double a = i * Math.PI * 2 / 4;
                    Vector v = new Vector(Math.cos(a) * width * 1.5, 0, Math.sin(a) * width * 1.5);
                    Location gLoc = pLoc.clone().add(v);
                    spawnParticle(gLoc.getWorld(), theme.getOrbitParticle(), gLoc, 1);
                }
            }
            
            // TRAIL particle along beam (every 1.5 distance)
            if (useTrailParticles && d % 1.5 < 0.3) {
                spawnTrailParticle(pLoc.getWorld(), pLoc, to, theme.getPrimaryColor(), 8);
            }
        }

        // Impact at target
        impact(player, theme, to, 2);
    }

    // ═════════════════════════════════════════════════════════════════════
    // SPIRAL EFFECTS (OPTIMIZED)
    // ════════════════════════════════════════════════════════════════════

    public void spiral(Player player, Theme theme, Location center, double maxRadius, int height, int rotations) {
        new BukkitRunnable() {
            double angle = 0;
            double currentHeight = 0;
            double currentRadius = 0;
            @Override
            public void run() {
                if (currentHeight > height) { cancel(); return; }

                // Fewer particles per rotation
                for (int r = 0; r < rotations; r++) {
                    double a = angle + r * Math.PI * 2 / rotations;
                    Vector v = new Vector(Math.cos(a) * currentRadius, currentHeight * 0.15, Math.sin(a) * currentRadius);
                    Location pLoc = center.clone().add(v);
                    spawnParticle(pLoc.getWorld(), theme.getCastParticle(), pLoc, 1);
                }

                angle += 0.35;
                currentHeight += 0.35;
                currentRadius = Math.min(maxRadius, currentHeight * 0.08);
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }

    // ═════════════════════════════════════════════════════════════════════
    // DOME/SHIELD EFFECTS (OPTIMIZED)
    // ════════════════════════════════════════════════════════════════════

    public void dome(Player player, Theme theme, Location center, double radius, int durationTicks) {
        new BukkitRunnable() {
            int tick = 0;
            @Override
            public void run() {
                if (tick >= durationTicks) { cancel(); return; }

                // Spherical dome - reduced resolution
                for (int phi = 0; phi <= 180; phi += 25) {
                    double y = Math.cos(Math.toRadians(phi)) * radius;
                    double r = Math.sin(Math.toRadians(phi)) * radius;
                    
                    for (int theta = 0; theta < 360; theta += 25) {
                        double x = Math.cos(Math.toRadians(theta + tick * 3)) * r;
                        double z = Math.sin(Math.toRadians(theta + tick * 3)) * r;
                        Location pLoc = center.clone().add(x, y, z);
                        spawnParticle(pLoc.getWorld(), theme.getAuraParticle(), pLoc, 1);
                    }
                }

                tick++;
            }
        }.runTaskTimer(plugin, 0L, 2L);
    }

    // ═════════════════════════════════════════════════════════════════════
    // ENTITY TRAIL - For projectiles/mobs (OPTIMIZED)
    // ════════════════════════════════════════════════════════════════════

    public void entityTrail(Entity entity, Theme theme, int durationTicks) {
        new BukkitRunnable() {
            int tick = 0;
            @Override
            public void run() {
                if (tick >= durationTicks || entity.isDead() || !entity.isValid()) { cancel(); return; }

                Location loc = entity.getLocation().add(0, entity.getHeight() / 2, 0);
                
                // Very minimal trail
                for (int i = 0; i < 2; i++) {
                    Vector v = new Vector(
                        (Math.random() - 0.5) * 0.3,
                        Math.random() * 0.3,
                        (Math.random() - 0.5) * 0.3
                    );
                    loc.add(v);
                    spawnParticle(loc.getWorld(), theme.getTrailParticle(), loc, 1);
                    loc.subtract(v);
                }

                tick++;
            }
        }.runTaskTimer(plugin, 0L, 2L);
    }

    // ═════════════════════════════════════════════════════════════════════
    // HEART BEAT PARTICLES - On player based on heart stability (OPTIMIZED)
    // ════════════════════════════════════════════════════════════════════

    public void heartBeatParticles(Player player, Theme theme, int stability) {
        Location loc = player.getLocation().add(0, 1.5, 0);
        
        // Frequency based on stability - capped
        int particles = Math.min(Math.max(1, (100 - stability) / 15), 6);
        
        for (int i = 0; i < particles; i++) {
            Vector v = new Vector(
                (Math.random() - 0.5) * 0.4,
                Math.random() * 0.4,
                (Math.random() - 0.5) * 0.4
            );
            loc.add(v);
            spawnParticle(loc.getWorld(), theme.getAuraParticle(), loc, 1);
            loc.subtract(v);
        }
        
        // Critical - soul particles (less frequent)
        if (stability < 20 && Math.random() < 0.3) {
            for (int i = 0; i < 3; i++) {
                Vector v = new Vector(
                    (Math.random() - 0.5) * 0.8,
                    Math.random() * 1.0,
                    (Math.random() - 0.5) * 0.8
                );
                loc.add(v);
                spawnParticle(loc.getWorld(), Particle.SOUL, loc, 1);
                loc.subtract(v);
            }
        }
        
        // Awakened - end rods
        if (stability >= 100 && Math.random() < 0.2) {
            for (int i = 0; i < 2; i++) {
                double a = Math.random() * Math.PI * 2;
                Vector v = new Vector(Math.cos(a) * 1.2, Math.random() * 1.0, Math.sin(a) * 1.2);
                loc.add(v);
                spawnParticle(loc.getWorld(), Particle.END_ROD, loc, 1);
                loc.subtract(v);
            }
        }
    }

    // ═════════════════════════════════════════════════════════════════════
    // METHODS CALLED BY VISUALEFFECTSMANAGER (OPTIMIZED)
    // ═════════════════════════════════════════════════════════════════════

    public void spawnEssenceGainParticles(Location location, Theme theme, int count, int quality) {
        int finalCount = clampCount(count * quality / 4);
        for (int i = 0; i < finalCount; i++) {
            double a = Math.random() * Math.PI * 2;
            double r = 1 + Math.random() * 1.5;
            Location p = location.clone().add(Math.cos(a) * r, Math.random() * 1.5, Math.sin(a) * r);
            spawnParticle(p.getWorld(), theme.getCastParticle(), p, 1, 0, 0.08, 0, 0.015);
        }
    }

    public void spawnKillEffect(Location location, Theme theme, int quality) {
        int finalCount = clampCount(20 * quality / 4);
        for (int i = 0; i < finalCount; i++) {
            double a = Math.random() * Math.PI * 2;
            double r = Math.random() * 2.5;
            Location p = location.clone().add(Math.cos(a) * r, Math.random() * 1.5, Math.sin(a) * r);
            spawnParticle(p.getWorld(), Particle.HAPPY_VILLAGER, p, 1, 0, 0.15, 0, 0.04);
        }
    }

    public void spawnDeathEffect(Location location, Theme theme, int quality) {
        int finalCount = clampCount(15 * quality / 4);
        for (int i = 0; i < finalCount; i++) {
            double a = Math.random() * Math.PI * 2;
            double r = Math.random() * 1.8;
            Location p = location.clone().add(Math.cos(a) * r, Math.random() * 1.2, Math.sin(a) * r);
            spawnParticle(p.getWorld(), Particle.SOUL, p, 1, 0, 0.08, 0, 0.015);
        }
    }

    public void spawnEventPhaseEffect(Location location, Theme theme, EventPhase phase, int quality) {
        int finalCount = clampCount(25 * quality / 4);
        for (int i = 0; i < finalCount; i++) {
            double a = Math.random() * Math.PI * 2;
            double r = 2.5 + Math.random() * 2.5;
            Location p = location.clone().add(Math.cos(a) * r, Math.random() * 2.5, Math.sin(a) * r);
            spawnParticle(p.getWorld(), theme.getUltimateParticle(), p, 1, 0, 0.15, 0, 0.04);
        }
    }

    public void spawnGlobalAnnouncementEffect(Theme theme, int quality) {
        World world = plugin.getServer().getWorlds().get(0);
        if (world == null) return;
        Location loc = world.getSpawnLocation();
        int finalCount = clampCount(30 * quality / 4);
        for (int i = 0; i < finalCount; i++) {
            double a = Math.random() * Math.PI * 2;
            double r = 4 + Math.random() * 8;
            Location p = loc.clone().add(Math.cos(a) * r, 4 + Math.random() * 8, Math.sin(a) * r);
            spawnParticle(p.getWorld(), theme.getUltimateParticle(), p, 1, 0, 0.2, 0, 0.08);
        }
    }

    public void spawnHeartDeathEffect(Location location, Theme theme, int quality) {
        new BukkitRunnable() {
            double radius = 4;
            @Override
            public void run() {
                if (radius < 0.5) { cancel(); return; }
                int particleCount = clampCount(12 * quality / 4);
                for (int i = 0; i < particleCount; i++) {
                    double a = Math.random() * Math.PI * 2;
                    double y = (Math.random() - 0.5) * 1.5;
                    Vector v = new Vector(Math.cos(a) * radius, y, Math.sin(a) * radius);
                    Location p = location.clone().add(v);
                    Vector toCenter = location.toVector().subtract(p.toVector()).normalize().multiply(0.18);
                    spawnParticle(p.getWorld(), theme.getImpactParticle(), p, 1, toCenter.getX(), toCenter.getY(), toCenter.getZ(), 0.04);
                }
                radius -= 0.25;
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }

    public void spawnAwakenedAura(Location location, Theme theme, int quality) {
        int finalCount = clampCount(10 * quality / 4);
        for (int i = 0; i < finalCount; i++) {
            double a = Math.random() * Math.PI * 2;
            double r = 1.2;
            Location p = location.clone().add(Math.cos(a) * r, Math.random() * 2.5, Math.sin(a) * r);
            spawnParticle(p.getWorld(), Particle.END_ROD, p, 1, 0, 0.08, 0, 0.01);
        }
    }

    // ═════════════════════════════════════════════════════════════════════
    // CLEAR ALL
    // ═════════════════════════════════════════════════════════════════════

    public void clearPlayer(Player player) {
        List<BukkitTask> tasks = activeAnimations.remove(player.getUniqueId());
        if (tasks != null) tasks.forEach(BukkitTask::cancel);
    }

    public void clearAll() {
        activeAnimations.values().forEach(list -> list.forEach(BukkitTask::cancel));
        activeAnimations.clear();
        if (tickResetTaskId != -1) {
            plugin.getServer().getScheduler().cancelTask(tickResetTaskId);
            tickResetTaskId = -1;
        }
    }

    public void shutdown() {
        clearAll();
    }
}