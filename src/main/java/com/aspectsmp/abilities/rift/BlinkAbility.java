package com.aspectsmp.abilities.rift;

import com.aspectsmp.abilities.BaseAbility;
import com.aspectsmp.core.Heart;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;

public class BlinkAbility extends BaseAbility {

    @Override
    public String getId() {
        return "rift:blink";
    }

    @Override
    public String getName() {
        return "Blink";
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

        org.bukkit.Location loc = player.getLocation().add(player.getLocation().getDirection().multiply(5));
        if (loc.getBlock().getType().isSolid()) return false;
        
        loc.getWorld().spawnParticle(Particle.PORTAL, player.getLocation().add(0, 1, 0), 30);
        loc.getWorld().spawnParticle(Particle.PORTAL, loc.add(0, 1, 0), 30);
        playSound(player, Sound.ENTITY_ENDERMAN_TELEPORT, 1.0f, 1.0f);
        
        loc.getWorld().playSound(loc, Sound.ENTITY_ENDERMAN_TELEPORT, 1.0f, 1.0f);
        player.teleport(loc);

        applyCooldown(player, getId(), 6000L);
        return true;
    }
}