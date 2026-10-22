package com.aspectsmp.abilities.storm;

import com.aspectsmp.abilities.BaseAbility;
import com.aspectsmp.core.Heart;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

public class CycloneAbility extends BaseAbility {

    @Override
    public String getId() {
        return "storm:cyclone";
    }

    @Override
    public String getName() {
        return "Cyclone";
    }

    @Override
    public int getTier() {
        return 3;
    }

    @Override
    public boolean isPassive() {
        return false;
    }

    @Override
    public boolean execute(Player player, Heart heart, Event triggerEvent) {
        if (!canExecute(player, heart)) return false;
        if (isOnCooldown(player, getId())) return false;

        org.bukkit.Location center = player.getLocation();
        player.getWorld().spawnParticle(Particle.CLOUD, center.add(0, 1, 0), 100, 4, 3, 4, 0.1);
        player.getWorld().spawnParticle(Particle.SWEEP_ATTACK, center.add(0, 1, 0), 50, 3, 2, 3, 0.05);
        playSound(player, Sound.ENTITY_BREEZE_WIND_BURST, 2.0f, 0.6f);

        new BukkitRunnable() {
            int ticks = 0;
            @Override
            public void run() {
                if (!player.isOnline() || ticks >= 100) { cancel(); return; }
                ticks += 10;
                
                // Expanding cyclone
                double radius = 2 + (ticks * 0.08);
                center.getWorld().spawnParticle(Particle.CLOUD, center, 80, radius, 2, radius, 0.05);
                center.getWorld().spawnParticle(Particle.SWEEP_ATTACK, center, 30, radius * 0.7, 1.5, radius * 0.7, 0.03);
                
                for (Entity entity : center.getWorld().getNearbyEntities(center, radius + 1, 4, radius + 1)) {
                    if (entity instanceof LivingEntity target && target != player && !(target instanceof org.bukkit.entity.Villager)) {
                        Vector toCenter = center.toVector().subtract(target.getLocation().toVector());
                        double dist = toCenter.length();
                        if (dist > 1) {
                            // Pull towards center + upward
                            Vector pull = toCenter.normalize().multiply(0.4).add(new Vector(0, 0.2, 0));
                            target.setVelocity(target.getVelocity().add(pull));
                            damageIgnoreArmor(target, 2.5 * getPowerMultiplier(player, heart), player);
                        }
                    }
                }
            }
        }.runTaskTimer(AspectSMP.getInstance(), 0L, 10L);

        // Player immunity to own cyclone
        player.addPotionEffect(new org.bukkit.potion.PotionEffect(
            org.bukkit.potion.PotionEffectType.LEVITATION, 120, 0, true, false), true);
        player.addPotionEffect(new org.bukkit.potion.PotionEffect(
            org.bukkit.potion.PotionEffectType.SLOW_FALLING, 120, 0), true);

        applyCooldown(player, getId(), getCooldownDuration(heart));
        return true;
    }

    @Override
    protected long getCooldownBase() {
        return 25000L;
    }
}