package com.aspectsmp.abilities.tide;

import com.aspectsmp.abilities.BaseAbility;
import com.aspectsmp.core.Heart;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;

public class TidalPrisonAbility extends BaseAbility {

    @Override
    public String getId() {
        return "tide:tidal_prison";
    }

    @Override
    public String getName() {
        return "Tidal Prison";
    }

    @Override
    public int getTier() {
        return 2;
    }

    @Override
    public boolean isPassive() {
        return false;
    }

    @Override
    public boolean execute(Player player, Heart heart, Event triggerEvent) {
        if (!canExecute(player, heart)) return false;
        if (isOnCooldown(player, getId())) return false;

        org.bukkit.Location loc = player.getLocation();
        player.getWorld().spawnParticle(Particle.WATER_BUBBLE, loc, 50, 2, 2, 2);
        playSound(player, Sound.ENTITY_DOLPHIN_PLAY, 1.0f, 1.0f);

        player.getWorld().getEntities().stream()
            .filter(e -> e instanceof Player && e != player && e.getLocation().distanceSquared(loc) <= 16)
            .forEach(e -> {
                if (e instanceof Player target) {
                    target.damage(4 * getPowerMultiplier(heart), player);
                }
            });

        applyCooldown(player, getId(), 15000L);
        return true;
    }
}