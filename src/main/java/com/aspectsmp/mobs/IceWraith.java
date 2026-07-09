package com.aspectsmp.mobs;

import com.aspectsmp.AspectSMP;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.entity.*;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;

public class IceWraith {

    private final AspectSMP plugin;
    private final LivingEntity entity;

    public IceWraith(AspectSMP plugin, Location location) {
        this.plugin = plugin;
        this.entity = location.getWorld().spawn(location, Phantom.class);
        entity.setCustomName("§f§lIce Wraith");
        entity.setCustomNameVisible(true);
        entity.getAttribute(org.bukkit.attribute.Attribute.MAX_HEALTH).setBaseValue(20.0);
        entity.setHealth(20.0);
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
                Player target = entity.getWorld().getNearbyEntities(entity.getLocation(), 15, 10, 15).stream()
                    .filter(e -> e instanceof Player)
                    .map(e -> (Player) e)
                    .findFirst()
                    .orElse(null);
                if (target != null) {
                    if (entity.getLocation().distanceSquared(target.getLocation()) < 25) {
                        entity.teleport(target.getLocation().add(2, 1, 2));
                        target.damage(4.0, entity);
                        target.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 60, 0));
                        target.getWorld().spawnParticle(org.bukkit.Particle.CRIT, target.getLocation().add(0, 1, 0), 20, 0.3, 0.5, 0.3, 0.05);
                        target.getWorld().playSound(target.getLocation(), Sound.ENTITY_PLAYER_HURT_FREEZE, 1.0f, 1.0f);
                    }
                }
            }
        }.runTaskTimer(plugin, 0L, 60L);
    }
}
