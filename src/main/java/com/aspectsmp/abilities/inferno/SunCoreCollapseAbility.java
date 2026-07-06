package com.aspectsmp.abilities.inferno;

import com.aspectsmp.abilities.BaseAbility;
import com.aspectsmp.core.Heart;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;

import java.util.List;

public class SunCoreCollapseAbility extends BaseAbility {

    @Override
    public String getId() {
        return "inferno:sun_core_collapse";
    }

    @Override
    public String getName() {
        return "Sun Core Collapse";
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

        for (int i = 0; i < 10; i++) {
            double d = i * 0.5;
            org.bukkit.scheduler.BukkitRunnable runnable = new org.bukkit.scheduler.BukkitRunnable() {
                @Override
                public void run() {
                    player.getWorld().spawnParticle(Particle.FLAME, player.getLocation().add(0, d, 0), 100, 2, 2, 2);
                }
            };
            runnable.runTaskLater(com.aspectsmp.AspectSMP.getInstance(), (long) d * 5);
        }
        
        playSound(player, Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 1.0f, 0.5f);

        List<Entity> nearby = player.getWorld().getEntities().stream()
            .filter(e -> e != player && e.getLocation().distanceSquared(player.getLocation()) <= 25)
            .toList();

        for (Entity entity : nearby) {
            if (entity instanceof Player target) {
                target.damage(10 * getPowerMultiplier(heart), player);
                target.setFireTicks(100);
            }
        }

        applyCooldown(player, getId(), 60000L);
        return true;
    }
}