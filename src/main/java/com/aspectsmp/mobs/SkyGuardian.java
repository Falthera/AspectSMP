package com.aspectsmp.mobs;

import com.aspectsmp.AspectSMP;
import org.bukkit.Location;
import org.bukkit.entity.*;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;

public class SkyGuardian {

    private final AspectSMP plugin;
    private final LivingEntity entity;

    public SkyGuardian(AspectSMP plugin, Location location) {
        this.plugin = plugin;
        this.entity = location.getWorld().spawn(location, Ravager.class);
        entity.setCustomName("§c§lSky Guardian");
        entity.setCustomNameVisible(true);
        entity.getAttribute(org.bukkit.attribute.Attribute.MAX_HEALTH).setBaseValue(200.0);
        entity.setHealth(200.0);
        entity.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, Integer.MAX_VALUE, 1, true, false));
        entity.addPotionEffect(new PotionEffect(PotionEffectType.RESISTANCE, Integer.MAX_VALUE, 2, true, false));
        startAI();
    }

    private void startAI() {
        new BukkitRunnable() {
            int tick = 0;
            @Override
            public void run() {
                if (entity.isDead()) {
                    cancel();
                    return;
                }
                tick++;
                Player target = entity.getWorld().getNearbyEntities(entity.getLocation(), 20, 15, 20).stream()
                    .filter(e -> e instanceof Player)
                    .map(e -> (Player) e)
                    .findFirst()
                    .orElse(null);
                if (target != null) {
                    switch (tick % 4) {
                        case 0 -> windCollapse(target);
                        case 1 -> stormPrison(target);
                        case 2 -> skyfall(target);
                        case 3 -> heavenBreak(target);
                    }
                }
            }
        }.runTaskTimer(plugin, 0L, 80L);
    }

    private void windCollapse(Player target) {
        target.setVelocity(target.getLocation().toVector().subtract(entity.getLocation().toVector()).normalize().multiply(2.5));
        target.getWorld().spawnParticle(org.bukkit.Particle.CLOUD, entity.getLocation().add(0, 1, 0), 50, 4, 2, 4, 0.05);
    }

    private void stormPrison(Player target) {
        target.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 80, 3));
        target.addPotionEffect(new PotionEffect(PotionEffectType.MINING_FATIGUE, 80, 2));
        target.getWorld().spawnParticle(org.bukkit.Particle.END_ROD, target.getLocation().add(0, 1, 0), 40, 2, 3, 2, 0.03);
    }

    private void skyfall(Player target) {
        target.setVelocity(new org.bukkit.util.Vector(0, 3, 0));
        target.getWorld().spawnParticle(org.bukkit.Particle.CLOUD, target.getLocation().add(0, 2, 0), 30, 1, 3, 1, 0.02);
    }

    private void heavenBreak(Player target) {
        target.damage(12.0, entity);
        target.getWorld().spawnParticle(org.bukkit.Particle.EXPLOSION_EMITTER, target.getLocation().add(0, 1, 0), 15, 3, 2, 3, 0.1);
        target.getWorld().playSound(target.getLocation(), Sound.ENTITY_BREEZE_WIND_BURST, 2.0f, 0.5f);
    }
}
