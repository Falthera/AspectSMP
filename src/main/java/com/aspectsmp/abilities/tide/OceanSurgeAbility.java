package com.aspectsmp.abilities.tide;

import com.aspectsmp.abilities.BaseAbility;
import com.aspectsmp.core.Heart;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

public class OceanSurgeAbility extends BaseAbility {

    @Override
    public String getId() {
        return "tide:ocean_surge";
    }

    @Override
    public String getName() {
        return "Whirlpool";
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

        Location center = player.getLocation();
        playSound(player, Sound.ENTITY_DOLPHIN_PLAY, 1.0f, 0.5f);

        new BukkitRunnable() {
            int ticks = 0;
            @Override
            public void run() {
                if (ticks >= 50) {
                    cancel();
                    return;
                }
                ticks += 10;
                center.getWorld().spawnParticle(Particle.BUBBLE_COLUMN_UP, center, 40, 4, 1, 4);
                center.getWorld().getEntities().stream()
                    .filter(e -> e instanceof Player && e != player && e.getLocation().distanceSquared(center) <= 25)
                    .forEach(e -> {
                        if (e instanceof Player target) {
                            Vector pull = center.toVector().subtract(target.getLocation().toVector()).normalize().multiply(0.6);
                            target.setVelocity(pull);
                            target.damage(4 * getPowerMultiplier(heart), player);
                            target.addPotionEffect(new org.bukkit.potion.PotionEffect(org.bukkit.potion.PotionEffectType.MINING_FATIGUE, 30, 2));
                        }
                    });
            }
        }.runTaskTimer(com.aspectsmp.AspectSMP.getInstance(), 0L, 10L);

        applyCooldown(player, getId(), 25000L);
        return true;
    }
}
