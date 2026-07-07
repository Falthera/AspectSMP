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

        Vector direction = player.getLocation().getDirection().normalize();
        playSound(player, Sound.ENTITY_DOLPHIN_PLAY, 1.0f, 0.5f);

        for (double i = 1; i <= 10; i += 0.5) {
            Location point = player.getLocation().add(direction.clone().multiply(i));
            point.getWorld().spawnParticle(Particle.SPLASH, point, 3, 0.3, 0.3, 0.3);
            point.getWorld().getEntities().stream()
                .filter(e -> e instanceof Player && e != player && e.getLocation().distanceSquared(point) <= 8)
                .forEach(e -> {
                    Vector knockback = direction.clone().multiply(1.5).add(new Vector(0, 0.3, 0));
                    e.setVelocity(e.getVelocity().add(knockback));
                    if (e instanceof Player target) {
                        target.damage(3 * getPowerMultiplier(player, heart), player);
                    }
                });
        }

        applyCooldown(player, getId(), 25000L);
        return true;
    }
}
