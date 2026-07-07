package com.aspectsmp.abilities.tempest;

import com.aspectsmp.abilities.BaseAbility;
import com.aspectsmp.core.Heart;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.util.Vector;

public class DashChainAbility extends BaseAbility {

    @Override
    public String getId() {
        return "tempest:dash_chain";
    }

    @Override
    public String getName() {
        return "Lightning Dash";
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

        Vector direction = player.getLocation().getDirection().multiply(1.2);
        player.setVelocity(direction);
        
        player.getWorld().spawnParticle(Particle.ELECTRIC_SPARK, player.getLocation(), 30, 0.5, 0.5, 0.5);
        playSound(player, Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 0.5f, 1.0f);

        applyCooldown(player, getId(), 5000L);
        return true;
    }
}