package com.aspectsmp.mobs;

import com.aspectsmp.AspectSMP;
import org.bukkit.Location;
import org.bukkit.entity.*;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;

public class TempestPhantom {

    private final AspectSMP plugin;
    private final LivingEntity entity;

    public TempestPhantom(AspectSMP plugin, Location location) {
        this.plugin = plugin;
        this.entity = location.getWorld().spawn(location, Phantom.class);
        entity.setCustomName("§9§lTempest Phantom");
        entity.setCustomNameVisible(true);
        entity.getAttribute(org.bukkit.attribute.Attribute.MAX_HEALTH).setBaseValue(25.0);
        entity.setHealth(25.0);
        entity.addPotionEffect(new PotionEffect(PotionEffectType.INVISIBILITY, Integer.MAX_VALUE, 0, true, false));
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
                if (entity.getTarget() instanceof Player target) {
                    if (entity.getLocation().distanceSquared(target.getLocation()) < 20) {
                        entity.teleport(target.getLocation().add(2, 1, 2));
                        target.damage(5.0, entity);
                        target.getWorld().spawnParticle(org.bukkit.Particle.ELECTRIC_SPARK, target.getLocation().add(0, 1, 0), 20);
                    }
                }
            }
        }.runTaskTimer(plugin, 0L, 60L);
    }
}
