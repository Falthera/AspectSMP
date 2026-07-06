package com.aspectsmp.abilities.cosmos;

import com.aspectsmp.abilities.BaseAbility;
import com.aspectsmp.core.Heart;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.util.Vector;

import java.util.List;

public class GravityPullAbility extends BaseAbility {

    @Override
    public String getId() {
        return "cosmos:gravity_pull";
    }

    @Override
    public String getName() {
        return "Gravity Pull";
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

        org.bukkit.Location loc = player.getLocation();
        player.getWorld().spawnParticle(Particle.PORTAL, loc, 50, 2, 2, 2);
        playSound(player, Sound.ENTITY_ENDER_DRAGON_HURT, 1.0f, 1.0f);

        List<Entity> nearby = loc.getWorld().getEntities().stream()
            .filter(e -> e != player && e.getLocation().distanceSquared(loc) <= 16)
            .toList();

        for (Entity entity : nearby) {
            Vector vec = loc.clone().add(0, 1, 0).toVector().subtract(entity.getLocation().toVector()).normalize().multiply(0.3);
            entity.setVelocity(entity.getVelocity().add(vec));
        }

        applyCooldown(player, getId(), 6000L);
        return true;
    }
}