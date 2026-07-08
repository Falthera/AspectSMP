package com.aspectsmp.mobs;

import com.aspectsmp.AspectSMP;
import org.bukkit.Location;
import org.bukkit.entity.*;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;

public class StormSentinel {

    private final AspectSMP plugin;
    private final LivingEntity entity;

    public StormSentinel(AspectSMP plugin, Location location) {
        this.plugin = plugin;
        this.entity = location.getWorld().spawn(location, Zoglin.class);
        entity.setCustomName("§e§lStorm Sentinel");
        entity.setCustomNameVisible(true);
        entity.getAttribute(org.bukkit.attribute.Attribute.MAX_HEALTH).setBaseValue(40.0);
        entity.setHealth(40.0);
        entity.addPotionEffect(new PotionEffect(PotionEffectType.RESISTANCE, Integer.MAX_VALUE, 1, true, false));
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
                Player target = entity.getWorld().getNearbyEntities(entity.getLocation(), 10, 10, 10).stream()
                    .filter(e -> e instanceof Player)
                    .map(e -> (Player) e)
                    .findFirst()
                    .orElse(null);
                if (target != null) {
                    if (entity.getLocation().distanceSquared(target.getLocation()) < 25) {
                        target.damage(6.0, entity);
                        target.setVelocity(target.getLocation().toVector().subtract(entity.getLocation().toVector()).normalize().multiply(0.8));
                    }
                }
            }
        }.runTaskTimer(plugin, 0L, 40L);
    }
}
