package com.aspectsmp.abilities.tide;

import com.aspectsmp.abilities.BaseAbility;
import com.aspectsmp.core.Heart;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.util.Vector;

import java.util.List;

public class HydroPushAbility extends BaseAbility {

    @Override
    public String getId() {
        return "tide:hydro_push";
    }

    @Override
    public String getName() {
        return "Hydro Push";
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

        Vector direction = player.getLocation().getDirection().multiply(1.5);
        player.getWorld().spawnParticle(Particle.BUBBLE, player.getLocation().add(direction), 30);
        playSound(player, Sound.ENTITY_DOLPHIN_SPLASH, 1.0f, 1.0f);

        List<Entity> nearby = player.getWorld().getEntities().stream()
            .filter(e -> e != player && e.getLocation().distanceSquared(player.getLocation()) <= 9)
            .toList();

        for (Entity entity : nearby) {
            entity.setVelocity(entity.getVelocity().add(direction).multiply(-2));
        }

        applyCooldown(player, getId(), 6000L);
        return true;
    }
}