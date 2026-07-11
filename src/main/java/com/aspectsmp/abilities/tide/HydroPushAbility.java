package com.aspectsmp.abilities.tide;

import com.aspectsmp.abilities.BaseAbility;
import com.aspectsmp.core.Heart;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.util.Vector;

public class HydroPushAbility extends BaseAbility {

    @Override
    public String getId() {
        return "tide:hydro_push";
    }

    @Override
    public String getName() {
        return "Tidal Wave";
    }

    @Override
    public int getTier() {
        return 1;
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
        player.getWorld().spawnParticle(Particle.SPLASH, player.getLocation().add(0, 1, 0), 40, 0.2, 0.2, 0.2);
        playSound(player, Sound.ENTITY_DOLPHIN_SPLASH, 1.0f, 1.0f);

        for (double i = 0.5; i <= 8; i += 0.5) {
            Location point = player.getLocation().add(direction.clone().multiply(i));
            point.getWorld().spawnParticle(Particle.BUBBLE, point, 2, 0.3, 0.3, 0.3);
            
            point.getWorld().getEntities().stream()
                .filter(e -> e instanceof Player && e != player)
                .forEach(e -> {
                    if (e.getLocation().distanceSquared(point) <= 4) {
                        Vector away = e.getLocation().toVector().subtract(point.toVector()).normalize();
                        e.setVelocity(e.getVelocity().add(away.multiply(0.8)));
                        if (e instanceof Player target) {
                            damageIgnoreArmor(target, 5 * getPowerMultiplier(player, heart), player);
                            target.addPotionEffect(new org.bukkit.potion.PotionEffect(org.bukkit.potion.PotionEffectType.SLOWNESS, 40, 1));
                        }
                    }
                });
        }

        applyCooldown(player, getId(), 10000L);
        return true;
    }
}