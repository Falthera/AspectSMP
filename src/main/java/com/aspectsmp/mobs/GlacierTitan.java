package com.aspectsmp.mobs;

import com.aspectsmp.AspectSMP;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.entity.*;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;

public class GlacierTitan {

    private final AspectSMP plugin;
    private final LivingEntity entity;

    public GlacierTitan(AspectSMP plugin, Location location) {
        this.plugin = plugin;
        this.entity = location.getWorld().spawn(location, Ravager.class);
        entity.setCustomName("§6§lGlacier Titan");
        entity.setCustomNameVisible(true);
        entity.getAttribute(org.bukkit.attribute.Attribute.MAX_HEALTH).setBaseValue(60.0);
        entity.setHealth(60.0);
        entity.addPotionEffect(new PotionEffect(PotionEffectType.RESISTANCE, Integer.MAX_VALUE, 2, true, false));
        startAI();
    }

    private void startAI() {
        new BukkitRunnable() {
            @Override
            public void run() {
                if (entity.isDead()) {
                    cancel();
                    return;
                }
                Player target = entity.getWorld().getNearbyEntities(entity.getLocation(), 10, 8, 10).stream()
                    .filter(e -> e instanceof Player)
                    .map(e -> (Player) e)
                    .findFirst()
                    .orElse(null);
                if (target != null) {
                    if (entity.getLocation().distanceSquared(target.getLocation()) < 36) {
                        target.damage(8.0, entity);
                        target.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 80, 1));
                        target.setVelocity(target.getLocation().toVector().subtract(entity.getLocation().toVector()).normalize().multiply(1.0));
                        target.getWorld().spawnParticle(org.bukkit.Particle.CLOUD, target.getLocation().add(0, 1, 0), 30, 1.0, 0.5, 1.0, 0.05);
                        target.getWorld().playSound(target.getLocation(), Sound.ENTITY_PLAYER_HURT_FREEZE, 1.5f, 0.8f);
                    }
                }
            }
        }.runTaskTimer(plugin, 0L, 60L);
    }
}
