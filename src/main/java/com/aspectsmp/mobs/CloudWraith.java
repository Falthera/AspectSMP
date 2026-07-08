package com.aspectsmp.mobs;

import com.aspectsmp.AspectSMP;
import org.bukkit.Location;
import org.bukkit.entity.*;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;

public class CloudWraith {

    private final AspectSMP plugin;
    private final LivingEntity entity;

    public CloudWraith(AspectSMP plugin, Location location) {
        this.plugin = plugin;
        this.entity = location.getWorld().spawn(location, Phantom.class);
        entity.setCustomName("§f§lCloud Wraith");
        entity.setCustomNameVisible(true);
        entity.getAttribute(org.bukkit.attribute.Attribute.MAX_HEALTH).setBaseValue(20.0);
        entity.setHealth(20.0);
        entity.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, Integer.MAX_VALUE, 1, true, false));
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
                    entity.setVelocity(entity.getLocation().getDirection().multiply(0.6));
                    if (entity.getLocation().distanceSquared(target.getLocation()) < 16) {
                        target.damage(3.0, entity);
                        target.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 40, 0));
                    }
                }
            }
        }.runTaskTimer(plugin, 0L, 20L);
    }
}
