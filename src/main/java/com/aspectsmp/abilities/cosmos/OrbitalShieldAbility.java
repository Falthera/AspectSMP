package com.aspectsmp.abilities.cosmos;

import com.aspectsmp.abilities.BaseAbility;
import com.aspectsmp.core.Heart;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;

public class OrbitalShieldAbility extends BaseAbility {

    @Override
    public String getId() {
        return "cosmos:orbital_shield";
    }

    @Override
    public String getName() {
        return "Orbital Shield";
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

        player.setInvulnerable(true);
        player.getWorld().spawnParticle(Particle.END_ROD, player.getLocation().add(0, 2, 0), 50, 1, 1, 1);
        playSound(player, Sound.BLOCK_BEACON_ACTIVATE, 1.0f, 1.0f);

        org.bukkit.scheduler.BukkitRunnable runnable = new org.bukkit.scheduler.BukkitRunnable() {
            int ticks = 0;
            @Override
            public void run() {
                ticks++;
                player.getWorld().spawnParticle(Particle.END_ROD, player.getLocation().add(0, 2, 0), 10, 0.5, 0.5, 0.5);
                if (ticks >= 100) {
                    player.setInvulnerable(false);
                    cancel();
                }
            }
        };
        runnable.runTaskTimer(com.aspectsmp.AspectSMP.getInstance(), 0L, 2L);

        applyCooldown(player, getId(), 20000L);
        return true;
    }
}