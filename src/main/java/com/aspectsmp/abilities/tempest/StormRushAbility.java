package com.aspectsmp.abilities.tempest;

import com.aspectsmp.abilities.BaseAbility;
import com.aspectsmp.core.Heart;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.util.Vector;

public class StormRushAbility extends BaseAbility {

    @Override
    public String getId() {
        return "tempest:storm_rush";
    }

    @Override
    public String getName() {
        return "Storm Rush";
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

        player.setInvulnerable(true);
        Vector direction = player.getLocation().getDirection().multiply(2.0);
        player.setVelocity(direction);

        player.getWorld().spawnParticle(Particle.ELECTRIC_SPARK, player.getLocation(), 100, 0.5, 0.5, 0.5);
        playSound(player, Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 1.0f, 1.0f);

        org.bukkit.scheduler.BukkitRunnable runnable = new org.bukkit.scheduler.BukkitRunnable() {
            @Override
            public void run() {
                player.setInvulnerable(false);
            }
        };
        runnable.runTaskLater(com.aspectsmp.AspectSMP.getInstance(), 40L);

        applyCooldown(player, getId(), 20000L);
        return true;
    }
}
