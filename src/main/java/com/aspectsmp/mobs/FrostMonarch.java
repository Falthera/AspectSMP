package com.aspectsmp.mobs;

import com.aspectsmp.AspectSMP;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.entity.*;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

public class FrostMonarch {

    private final AspectSMP plugin;
    private final LivingEntity entity;

    public FrostMonarch(AspectSMP plugin, Location location) {
        this.plugin = plugin;
        this.entity = location.getWorld().spawn(location, Ravager.class);
        entity.setCustomName("§b§lFrost Monarch");
        entity.setCustomNameVisible(true);
        entity.getAttribute(org.bukkit.attribute.Attribute.MAX_HEALTH).setBaseValue(200.0);
        entity.setHealth(200.0);
        entity.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, Integer.MAX_VALUE, 1, true, false));
        entity.addPotionEffect(new PotionEffect(PotionEffectType.RESISTANCE, Integer.MAX_VALUE, 2, true, false));
        startAI();
    }

    public LivingEntity getEntity() {
        return entity;
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
                Player target = entity.getWorld().getNearbyEntities(entity.getLocation(), 25, 20, 25).stream()
                    .filter(e -> e instanceof Player)
                    .map(e -> (Player) e)
                    .findFirst()
                    .orElse(null);
                if (target != null) {
                    switch (tick % 4) {
                        case 0 -> icefall(target);
                        case 1 -> frozenPrison(target);
                        case 2 -> wintersGrasp(target);
                        case 3 -> eternalWinter(target);
                    }
                }
            }
        }.runTaskTimer(plugin, 0L, 80L);
    }

    private void icefall(Player target) {
        target.damage(6.0, entity);
        target.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 80, 1));
        target.getWorld().spawnParticle(org.bukkit.Particle.CRIT, target.getLocation().add(0, 1, 0), 40, 2.0, 3.0, 2.0, 0.1);
        target.getWorld().playSound(target.getLocation(), Sound.BLOCK_POINTED_DRIPSTONE_LAND, 2.0f, 0.6f);
    }

    private void frozenPrison(Player target) {
        target.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 100, 3));
        target.getWorld().spawnParticle(org.bukkit.Particle.CRIT, target.getLocation(), 60, 1.5, 1.5, 1.5, 0.1);
        target.getWorld().playSound(target.getLocation(), Sound.BLOCK_GLASS_BREAK, 1.5f, 0.5f);
    }

    private void wintersGrasp(Player target) {
        Vector pull = entity.getLocation().toVector().subtract(target.getLocation().toVector()).normalize().multiply(1.5);
        target.setVelocity(pull);
        target.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 60, 1));
        target.getWorld().spawnParticle(org.bukkit.Particle.CLOUD, target.getLocation().add(0, 1, 0), 30, 1.0, 1.0, 1.0, 0.02);
        target.getWorld().playSound(target.getLocation(), Sound.ENTITY_PLAYER_HURT_FREEZE, 1.5f, 0.8f);
    }

    private void eternalWinter(Player target) {
        target.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 120, 3));
        target.addPotionEffect(new PotionEffect(PotionEffectType.MINING_FATIGUE, 120, 1));
        target.setFreezeTicks(120);
        target.getWorld().spawnParticle(org.bukkit.Particle.END_ROD, target.getLocation().add(0, 1, 0), 60, 2.0, 2.0, 2.0, 0.02);
        target.getWorld().playSound(target.getLocation(), Sound.ENTITY_PLAYER_HURT_FREEZE, 2.0f, 0.4f);
    }
}
