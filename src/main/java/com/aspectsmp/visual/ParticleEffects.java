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

public class ParticleEffects {

    private final AspectSMP plugin;
    private final Map<UUID, List<BukkitTask>> activeAnimations = new ConcurrentHashMap<>();
    private final boolean useTrailParticles;

    public ParticleEffects(AspectSMP plugin) {
        this.plugin = plugin;
        this.useTrailParticles = plugin.getConfig().getBoolean("visual-effects.use-trail-particles", true);
    }

    // ════════════════════════════════════════════════════════════════════
    // CORE PARTICLE HELPERS - 1.21.11 Particle.Builder API
    // ════════════════════════════════════════════════════════════════════

    private Particle.DustOptions dust(Color color, float size) {
        return new Particle.DustOptions(color, size);
    }

    private Particle.DustOptions dust(Color color) {
        return dust(color, 1.0f);
    }

    private void spawnParticle(World world, Particle particle, Location location, int count) {
        particle.builder().location(location).count(count).spawn();
    }

    private void spawnParticle(World world, Particle particle, Location location, int count, double offsetX, double offsetY, double offsetZ, double extra) {
        particle.builder()
            .location(location)
            .count(count)
            .offset(offsetX, offsetY, offsetZ)
            .extra(extra)
            .spawn();
    }

    private void spawnParticle(World world, Particle particle, Location location, int count, double offsetX, double offsetY, double offsetZ, double extra, Particle.DustOptions dust) {
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
        Particle.TRAIL.builder()
            .location(from)
            .count(1)
            .trail(new Particle.Trail(to, color, duration))
            .spawn();
    }

    // ════════════════════════════════════════════════════════════════════
    // AURA EFFECTS - Persistent around player
    // ════════════════════════════════════════════════════════════════════

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

                // Base aura ring
                for (int i = 0; i < t.getAuraParticleCount(); i++) {
                    double a = angle + (i * Math.PI * 2 / t.getAuraParticleCount());
                    double radius = 1.2 + Math.sin(tick * 0.1 + i) * 0.3;
                    Vector v = new Vector(Math.cos(a) * radius, 0, Math.sin(a) * radius);
                    loc.add(v);
                    spawnParticle(loc.getWorld(), auraParticle, loc, 1);
                    loc.subtract(v);
                }

                // Stability-based secondary ring
                if (currentStability < 50) {
                    for (int i = 0; i < 4; i++) {
                        double a = angle + Math.PI / 2 + (i * Math.PI / 2);
                        double radius = 0.8;
                        Vector v = new Vector(Math.cos(a) * radius, height, Math.sin(a) * radius);
                        loc.add(v);
                        spawnParticle(loc.getWorld(), t.getOrbitParticle(), loc, 1);
                        loc.subtract(v);
                    }
                }

                // Critical stability - inner chaos ring
                if (currentStability < 20) {
                    for (int i = 0; i < 6; i++) {
                        double a = -angle * 1.5 + (i * Math.PI / 3);
                        double radius = 0.5 + Math.random() * 0.3;
                        Vector v = new Vector(Math.cos(a) * radius, (Math.random() - 0.5) * 1.5, Math.sin(a) * radius);
                        loc.add(v);
                        spawnParticle(loc.getWorld(), Particle.SOUL_FIRE_FLAME, loc, 1);
                        loc.subtract(v);
                    }
                }

                // Awakened - golden particles rising
                if (currentStability >= 100) {
                    for (int i = 0; i < 3; i++) {
                        double a = angle + (i * Math.PI * 2 / 3);
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

    // ════════════════════════════════════════════════════════════════════
    // ORBIT EFFECTS - Particles orbiting player
    // ════════════════════════════════════════════════════════════════════

    public void startOrbitEffect(Player player, Theme theme, int tier) {
        UUID id = player.getUniqueId();
        
        BukkitTask task = new BukkitRunnable() {
            double angle = 0;
            double verticalAngle = 0;
            @Override
            public void run() {
                if (!player.isOnline() || player.isDead()) { cancel(); return; }

                Location loc = player.getLocation().add(0, 1.5, 0);
                int particles = 4 + tier * 2;

                for (int i = 0; i < particles; i++) {
                    double a = angle + (i * Math.PI * 2 / particles);
                    double radius = 1.0 + tier * 0.3;
                    double y = Math.sin(verticalAngle + i) * 0.5;
                    
                    Vector v = new Vector(Math.cos(a) * radius, y, Math.sin(a) * radius);
                    loc.add(v);
                    spawnParticle(loc.getWorld(), theme.getOrbitParticle(), loc, 1);
                    loc.subtract(v);
                }

                angle += theme.getOrbitSpeed();
                verticalAngle += 0.02;
            }
        }.runTaskTimer(plugin, 0L, 1L);

        activeAnimations.computeIfAbsent(id, k -> new ArrayList<>()).add(task);
    }

    // ════════════════════════════════════════════════════════════════════
    // TRAIL EFFECTS - Behind moving player (1.21.11 TRAIL particle)
    // ════════════════════════════════════════════════════════════════════

    public void startTrailEffect(Player player, Theme theme) {
        UUID id = player.getUniqueId();
        Location lastLoc = player.getLocation();

        BukkitTask task = new BukkitRunnable() {
            @Override
            public void run() {
                if (!player.isOnline() || player.isDead()) { cancel(); return; }

                Location loc = player.getLocation();
                
                // Only spawn if moved
                if (loc.distance(lastLoc) > 0.2) {
                    
                    for (int i = 0; i < theme.getTrailParticleCount(); i++) {
                        Vector offset = new Vector(
                            (Math.random() - 0.5) * 0.5,
                            Math.random() * 1.5,
                            (Math.random() - 0.5) * 0.5
                        );
                        loc.add(offset);
                        spawnParticle(loc.getWorld(), theme.getTrailParticle(), loc, 1);
                        loc.subtract(offset);
                    }
                    
                    // 1.21.11 TRAIL particle toward last location
                    if (useTrailParticles && theme.getTrailParticleAlt() != null) {
                        spawnTrailParticle(loc.getWorld(), loc, lastLoc, theme.getPrimaryColor(), 20);
                    }
                    
                    lastLoc = loc.clone();
                }
            }
        }.runTaskTimer(plugin, 0L, 2L);

        activeAnimations.computeIfAbsent(id, k -> new ArrayList<>()).add(task);
    }

    public void stopTrailEffect(Player player) {
        // Handled by stopPassiveAura since we use same map
    }

    // ════════════════════════════════════════════════════════════════════
    // CAST EFFECTS - Ability charging/casting
    // ═══════════════════════════════════════════════════════════════════

    public void castCharge(Player player, Theme theme, int tier, int durationTicks) {
        Location loc = player.getLocation().add(0, 1, 0);
        Color primary = theme.getPrimaryColor();
        Color secondary = theme.getSecondaryColor();

        new BukkitRunnable() {
            int tick = 0;
            @Override
            public void run() {
                if (!player.isOnline() || tick >= durationTicks) { cancel(); return; }

                float progress = (float) tick / durationTicks;
                int particles = (int) (10 * progress * tier);

                // Converging particles
                for (int i = 0; i < particles; i++) {
                    double angle = Math.random() * Math.PI * 2;
                    double radius = (1.0 - progress) * 2.0 + 0.3;
                    double y = Math.random() * 2.0;
                    
                    Vector v = new Vector(Math.cos(angle) * radius, y, Math.sin(angle) * radius);
                    Location pLoc = loc.clone().add(v);
                    
                    // Particles move toward center
                    Vector toCenter = loc.toVector().subtract(pLoc.toVector()).normalize().multiply(0.1);
                    
                    spawnParticle(pLoc.getWorld(), theme.getCastParticle(), pLoc, 1);
                }

                // Ground ring expanding
                if (tick % 3 == 0) {
                    double ringRadius = progress * 3.0;
                    for (int i = 0; i < 16; i++) {
                        double a = i * Math.PI * 2 / 16;
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

        // Explosive burst
        for (int ring = 0; ring < 3; ring++) {
            final int r = ring;
            new BukkitRunnable() {
                @Override
                public void run() {
                    double radius = 0.5 + r * 0.5;
                    for (int i = 0; i < 20 + tier * 5; i++) {
                        double a = Math.random() * Math.PI * 2;
                        double y = (Math.random() - 0.5) * 2.0;
                        Vector v = new Vector(Math.cos(a) * radius, y, Math.sin(a) * radius);
                        Location pLoc = loc.clone().add(v);
                        Vector velocity = v.clone().normalize().multiply(0.5 + r * 0.2);
                        spawnParticle(pLoc.getWorld(), theme.getCastParticle(), pLoc, 1, 
                            velocity.getX(), velocity.getY(), velocity.getZ(), 0.05);
                    }
                }
            }.runTaskLater(plugin, r * 2L);
        }

        // Vertical beam
        for (int i = 0; i < 30; i++) {
            double y = i * 0.2;
            Location pLoc = loc.clone().add(0, y, 0);
            spawnParticle(pLoc.getWorld(), theme.getCastParticle(), pLoc, 3, 0.3, 0, 0.3, 0.02);
        }
    }

    // ════════════════════════════════════════════════════════════════════
    // IMPACT EFFECTS - On hit/landing
    // ═══════════════════════════════════════════════════════════════════

    public void impact(Player player, Theme theme, Location location, int tier) {
        Color primary = theme.getPrimaryColor();

        // Main impact burst
        for (int i = 0; i < 15 + tier * 5; i++) {
            Vector v = new Vector(
                (Math.random() - 0.5) * 2,
                Math.random() * 2,
                (Math.random() - 0.5) * 2
            ).normalize().multiply(0.1 + Math.random() * 0.3);
            
            spawnParticle(location.getWorld(), theme.getImpactParticle(), location, 1, 
                v.getX(), v.getY(), v.getZ(), 0.1);
        }

        // Ground shockwave
        new BukkitRunnable() {
            double radius = 0;
            @Override
            public void run() {
                if (radius > 3 + tier) { cancel(); return; }
                
                for (int i = 0; i < 20; i++) {
                    double a = i * Math.PI * 2 / 20;
                    Vector v = new Vector(Math.cos(a) * radius, 0.1, Math.sin(a) * radius);
                    Location pLoc = location.clone().add(v);
                    spawnParticle(pLoc.getWorld(), theme.getImpactParticle(), pLoc, 1);
                }
                radius += 0.5;
            }
        }.runTaskTimer(plugin, 0L, 1L);

        // Vertical pillar
        for (int i = 0; i < 20; i++) {
            double y = i * 0.2;
            Location pLoc = location.clone().add(0, y, 0);
            spawnParticle(pLoc.getWorld(), theme.getImpactParticle(), pLoc, 2, 0.2, 0, 0.2, 0.02);
        }
    }

    public void impactDirectional(Player player, Theme theme, Location location, Vector direction, int tier) {
        // Cone-shaped impact in direction of attack
        for (int i = 0; i < 20 + tier * 5; i++) {
            double spread = 0.5; // 90 degree cone
            double angle = Math.atan2(direction.getZ(), direction.getX()) + (Math.random() - 0.5) * spread;
            double y = Math.random() * 0.5;
            double speed = 0.2 + Math.random() * 0.5;
            
            Vector v = new Vector(Math.cos(angle) * speed, y, Math.sin(angle) * speed);
            spawnParticle(location.getWorld(), theme.getImpactParticle(), location, 1, 
                v.getX(), v.getY(), v.getZ(), 0.1);
        }
    }

    // ════════════════════════════════════════════════════════════════════
    // ULTIMATE EFFECTS - Major abilities
    // ═══════════════════════════════════════════════════════════════════

    public void ultimate(Player player, Theme theme) {
        Location loc = player.getLocation().add(0, 1, 0);
        Color primary = theme.getPrimaryColor();
        Color accent = theme.getAccentColor();

        // Stage 1: Implosion
        new BukkitRunnable() {
            double radius = 5;
            @Override
            public void run() {
                if (radius < 0.5) { cancel(); return; }
                
                for (int i = 0; i < 30; i++) {
                    double a = Math.random() * Math.PI * 2;
                    double y = (Math.random() - 0.5) * 2;
                    Vector v = new Vector(Math.cos(a) * radius, y, Math.sin(a) * radius);
                    Location pLoc = loc.clone().add(v);
                    Vector toCenter = loc.toVector().subtract(pLoc.toVector()).normalize().multiply(0.3);
                    spawnParticle(pLoc.getWorld(), theme.getUltimateParticle(), pLoc, 1,
                        toCenter.getX(), toCenter.getY(), toCenter.getZ(), 0.05);
                }
                radius -= 0.3;
            }
        }.runTaskTimer(plugin, 0L, 1L);

        // Stage 2: Explosion (delayed)
        new BukkitRunnable() {
            @Override
            public void run() {
                // Massive sphere
                for (int layer = 0; layer < 5; layer++) {
                    double r = 1 + layer * 0.8;
                    for (int i = 0; i < 40; i++) {
                        double a = Math.random() * Math.PI * 2;
                        double y = (Math.random() - 0.5) * 2;
                        Vector v = new Vector(Math.cos(a) * r, y, Math.sin(a) * r);
                        Location pLoc = loc.clone().add(v);
                        Vector vel = v.clone().normalize().multiply(0.5 + layer * 0.1);
                        spawnParticle(pLoc.getWorld(), theme.getUltimateParticle(), pLoc, 1,
                            vel.getX(), vel.getY(), vel.getZ(), 0.1);
                    }
                }

                // Rising pillar
                new BukkitRunnable() {
                    int height = 0;
                    @Override
                    public void run() {
                        if (height > 50) { cancel(); return; }
                        for (int i = 0; i < 5; i++) {
                            double a = Math.random() * Math.PI * 2;
                            double r = 1.5;
                            Vector v = new Vector(Math.cos(a) * r, height * 0.2, Math.sin(a) * r);
                            Location pLoc = loc.clone().add(v);
                            spawnParticle(pLoc.getWorld(), theme.getUltimateParticle(), pLoc, 1, 0, 0.1, 0, 0);
                        }
                        height++;
                    }
                }.runTaskTimer(plugin, 0L, 1L);

                // Shockwave ring
                new BukkitRunnable() {
                    double radius = 0;
                    @Override
                    public void run() {
                        if (radius > 20) { cancel(); return; }
                        for (int i = 0; i < 60; i++) {
                            double a = i * Math.PI * 2 / 60;
                            Vector v = new Vector(Math.cos(a) * radius, 0.1, Math.sin(a) * radius);
                            Location pLoc = loc.clone().add(v);
                            spawnParticle(pLoc.getWorld(), Particle.SONIC_BOOM, pLoc, 1);
                        }
                        radius += 1.0;
                    }
                }.runTaskTimer(plugin, 0L, 1L);
            }
        }.runTaskLater(plugin, 20L);
    }

    // ════════════════════════════════════════════════════════════════════
    // BEAM EFFECTS - Directed abilities (1.21.11 TRAIL particle)
    // ═══════════════════════════════════════════════════════════════════

    public void beam(Player player, Theme theme, Location from, Location to, double width) {
        Vector direction = to.toVector().subtract(from.toVector());
        double distance = direction.length();
        direction.normalize();

        for (double d = 0; d < distance; d += 0.2) {
            Location pLoc = from.clone().add(direction.clone().multiply(d));
            
            // Core beam
            for (int i = 0; i < 3; i++) {
                Vector offset = new Vector(
                    (Math.random() - 0.5) * width,
                    (Math.random() - 0.5) * width,
                    (Math.random() - 0.5) * width
                );
                pLoc.add(offset);
                spawnParticle(pLoc.getWorld(), theme.getCastParticle(), pLoc, 1);
                pLoc.subtract(offset);
            }
            
            // Outer glow
            if (d % 0.5 < 0.2) {
                for (int i = 0; i < 8; i++) {
                    double a = i * Math.PI * 2 / 8;
                    Vector v = new Vector(Math.cos(a) * width * 2, 0, Math.sin(a) * width * 2);
                    Location gLoc = pLoc.clone().add(v);
                    spawnParticle(gLoc.getWorld(), theme.getOrbitParticle(), gLoc, 1);
                }
            }
            
            // 1.21.11 TRAIL particle along beam
            if (useTrailParticles && d % 1.0 < 0.2) {
                spawnTrailParticle(pLoc.getWorld(), pLoc, to, theme.getPrimaryColor(), 10);
            }
        }

        // Impact at target
        impact(player, theme, to, 3);
    }

    // ════════════════════════════════════════════════════════════════════
    // SPIRAL EFFECTS
    // ═══════════════════════════════════════════════════════════════════

    public void spiral(Player player, Theme theme, Location center, double maxRadius, int height, int rotations) {
        new BukkitRunnable() {
            double angle = 0;
            double currentHeight = 0;
            double currentRadius = 0;
            @Override
            public void run() {
                if (currentHeight > height) { cancel(); return; }

                for (int r = 0; r < rotations; r++) {
                    double a = angle + r * Math.PI * 2 / rotations;
                    Vector v = new Vector(Math.cos(a) * currentRadius, currentHeight * 0.2, Math.sin(a) * currentRadius);
                    Location pLoc = center.clone().add(v);
                    spawnParticle(pLoc.getWorld(), theme.getCastParticle(), pLoc, 1);
                }

                angle += 0.5;
                currentHeight += 0.5;
                currentRadius = Math.min(maxRadius, currentHeight * 0.1);
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }

    // ═══════════════════════════════════════════════════════════════════
    // DOME/SHIELD EFFECTS
    // ══════════════════════════════════════════════════════════════════

    public void dome(Player player, Theme theme, Location center, double radius, int durationTicks) {
        new BukkitRunnable() {
            int tick = 0;
            @Override
            public void run() {
                if (tick >= durationTicks) { cancel(); return; }

                // Spherical dome
                for (int phi = 0; phi <= 180; phi += 15) {
                    double y = Math.cos(Math.toRadians(phi)) * radius;
                    double r = Math.sin(Math.toRadians(phi)) * radius;
                    
                    for (int theta = 0; theta < 360; theta += 15) {
                        double x = Math.cos(Math.toRadians(theta + tick * 2)) * r;
                        double z = Math.sin(Math.toRadians(theta + tick * 2)) * r;
                        Location pLoc = center.clone().add(x, y, z);
                        spawnParticle(pLoc.getWorld(), theme.getAuraParticle(), pLoc, 1);
                    }
                }

                tick++;
            }
        }.runTaskTimer(plugin, 0L, 2L);
    }

    // ═══════════════════════════════════════════════════════════════════
    // ENTITY TRAIL - For projectiles/mobs
    // ══════════════════════════════════════════════════════════════════

    public void entityTrail(Entity entity, Theme theme, int durationTicks) {
        new BukkitRunnable() {
            int tick = 0;
            @Override
            public void run() {
                if (tick >= durationTicks || entity.isDead() || !entity.isValid()) { cancel(); return; }

                Location loc = entity.getLocation().add(0, entity.getHeight() / 2, 0);
                
                for (int i = 0; i < 3; i++) {
                    Vector v = new Vector(
                        (Math.random() - 0.5) * 0.5,
                        Math.random() * 0.5,
                        (Math.random() - 0.5) * 0.5
                    );
                    loc.add(v);
                    spawnParticle(loc.getWorld(), theme.getTrailParticle(), loc, 1);
                    loc.subtract(v);
                }

                tick++;
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }

    // ═══════════════════════════════════════════════════════════════════
    // HEART BEAT PARTICLES - On player based on heart stability
    // ═════════════════════════════════════════════════════════════════

    public void heartBeatParticles(Player player, Theme theme, int stability) {
        Location loc = player.getLocation().add(0, 1.5, 0);
        Color color = theme.getPrimaryColor();
        
        // Frequency based on stability
        int particles = Math.max(1, (100 - stability) / 10);
        
        for (int i = 0; i < particles; i++) {
            Vector v = new Vector(
                (Math.random() - 0.5) * 0.5,
                Math.random() * 0.5,
                (Math.random() - 0.5) * 0.5
            );
            loc.add(v);
            spawnParticle(loc.getWorld(), theme.getAuraParticle(), loc, 1);
            loc.subtract(v);
        }
        
        // Critical - soul particles
        if (stability < 20) {
            for (int i = 0; i < 5; i++) {
                Vector v = new Vector(
                    (Math.random() - 0.5) * 1.0,
                    Math.random() * 1.5,
                    (Math.random() - 0.5) * 1.0
                );
                loc.add(v);
                spawnParticle(loc.getWorld(), Particle.SOUL, loc, 1);
                loc.subtract(v);
            }
        }
        
// Awakened - end rods
        if (stability >= 100) {
            for (int i = 0; i < 3; i++) {
                double a = Math.random() * Math.PI * 2;
                Vector v = new Vector(Math.cos(a) * 1.5, Math.random() * 1.5, Math.sin(a) * 1.5);
                loc.add(v);
                spawnParticle(loc.getWorld(), Particle.END_ROD, loc, 1);
                loc.subtract(v);
            }
        }
    }

    // ════════════════════════════════════════════════════════════════════
    // METHODS CALLED BY VISUALEFFECTSMANAGER
    // ════════════════════════════════════════════════════════════════════

    public void spawnEssenceGainParticles(Location location, Theme theme, int count, int quality) {
        for (int i = 0; i < count * quality / 3; i++) {
            double a = Math.random() * Math.PI * 2;
            double r = 1 + Math.random() * 2;
            Location p = location.clone().add(Math.cos(a) * r, Math.random() * 2, Math.sin(a) * r);
            spawnParticle(p.getWorld(), theme.getCastParticle(), p, 1, 0, 0.1, 0, 0.02);
        }
    }

    public void spawnKillEffect(Location location, Theme theme, int quality) {
        // Golden explosion for kills
        for (int i = 0; i < 30 * quality / 3; i++) {
            double a = Math.random() * Math.PI * 2;
            double r = Math.random() * 3;
            Location p = location.clone().add(Math.cos(a) * r, Math.random() * 2, Math.sin(a) * r);
            spawnParticle(p.getWorld(), Particle.HAPPY_VILLAGER, p, 1, 0, 0.2, 0, 0.05);
        }
    }

    public void spawnDeathEffect(Location location, Theme theme, int quality) {
        // Dark death particles
        for (int i = 0; i < 20 * quality / 3; i++) {
            double a = Math.random() * Math.PI * 2;
            double r = Math.random() * 2;
            Location p = location.clone().add(Math.cos(a) * r, Math.random() * 1.5, Math.sin(a) * r);
            spawnParticle(p.getWorld(), Particle.SOUL, p, 1, 0, 0.1, 0, 0.02);
        }
    }

    public void spawnEventPhaseEffect(Location location, Theme theme, EventPhase phase, int quality) {
        // Phase transition particles
        for (int i = 0; i < 40 * quality / 3; i++) {
            double a = Math.random() * Math.PI * 2;
            double r = 3 + Math.random() * 3;
            Location p = location.clone().add(Math.cos(a) * r, Math.random() * 3, Math.sin(a) * r);
            spawnParticle(p.getWorld(), theme.getUltimateParticle(), p, 1, 0, 0.2, 0, 0.05);
        }
    }

    public void spawnGlobalAnnouncementEffect(Theme theme, int quality) {
        // Global effect at spawn
        World world = plugin.getServer().getWorlds().get(0);
        Location loc = world.getSpawnLocation();
        for (int i = 0; i < 50 * quality / 3; i++) {
            double a = Math.random() * Math.PI * 2;
            double r = 5 + Math.random() * 10;
            Location p = loc.clone().add(Math.cos(a) * r, 5 + Math.random() * 10, Math.sin(a) * r);
            spawnParticle(p.getWorld(), theme.getUltimateParticle(), p, 1, 0, 0.3, 0, 0.1);
        }
    }

    public void spawnHeartDeathEffect(Location location, Theme theme, int quality) {
        // Heart death - dramatic implosion then explosion
        new BukkitRunnable() {
            double radius = 5;
            @Override
            public void run() {
                if (radius < 0.5) { cancel(); return; }
                for (int i = 0; i < 20 * quality / 3; i++) {
                    double a = Math.random() * Math.PI * 2;
                    double y = (Math.random() - 0.5) * 2;
                    Vector v = new Vector(Math.cos(a) * radius, y, Math.sin(a) * radius);
                    Location p = location.clone().add(v);
                    Vector toCenter = location.toVector().subtract(p.toVector()).normalize().multiply(0.2);
                    spawnParticle(p.getWorld(), theme.getImpactParticle(), p, 1, toCenter.getX(), toCenter.getY(), toCenter.getZ(), 0.05);
                }
                radius -= 0.3;
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }

    public void spawnAwakenedAura(Location location, Theme theme, int quality) {
        // Golden awakened particles rising
        for (int i = 0; i < 15 * quality / 3; i++) {
            double a = Math.random() * Math.PI * 2;
            double r = 1.5;
            Location p = location.clone().add(Math.cos(a) * r, Math.random() * 3, Math.sin(a) * r);
            spawnParticle(p.getWorld(), Particle.END_ROD, p, 1, 0, 0.1, 0, 0.01);
        }
    }

    // ════════════════════════════════════════════════════════════════════
    // CLEAR ALL
    // ═══════════════════════════════════════════════════════════════════

    public void clearPlayer(Player player) {
        List<BukkitTask> tasks = activeAnimations.remove(player.getUniqueId());
        if (tasks != null) tasks.forEach(BukkitTask::cancel);
    }

    public void clearAll() {
        activeAnimations.values().forEach(list -> list.forEach(BukkitTask::cancel));
        activeAnimations.clear();
    }
}