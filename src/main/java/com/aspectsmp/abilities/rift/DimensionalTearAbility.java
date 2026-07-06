package com.aspectsmp.abilities.rift;

import com.aspectsmp.abilities.BaseAbility;
import com.aspectsmp.core.Heart;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;

import java.util.List;
import java.util.UUID;

public class DimensionalTearAbility extends BaseAbility {

    @Override
    public String getId() {
        return "rift:dimensional_tear";
    }

    @Override
    public String getName() {
        return "Dimensional Tear";
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

        org.bukkit.Location loc = player.getLocation();
        loc.getWorld().spawnParticle(Particle.DRIPPING_OBSIDIAN_TEAR, loc, 100, 2, 2, 2);
        playSound(player, Sound.BLOCK_PORTAL_AMBIENT, 1.0f, 1.0f);

        List<Entity> nearby = loc.getWorld().getEntities().stream()
            .filter(e -> e instanceof Player && e != player && e.getLocation().distanceSquared(loc) <= 16)
            .toList();

        for (Entity entity : nearby) {
            if (entity instanceof Player target) {
                target.damage(6 * getPowerMultiplier(heart), player);
            }
        }

        applyCooldown(player, getId(), 25000L);
        return true;
    }
}