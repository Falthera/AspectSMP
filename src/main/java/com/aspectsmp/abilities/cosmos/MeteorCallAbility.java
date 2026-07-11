package com.aspectsmp.abilities.cosmos;

import com.aspectsmp.abilities.BaseAbility;
import com.aspectsmp.core.Heart;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;

import java.util.List;

public class MeteorCallAbility extends BaseAbility {

    @Override
    public String getId() {
        return "cosmos:meteor_call";
    }

    @Override
    public String getName() {
        return "Meteor Call";
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

        org.bukkit.Location targetLoc = player.getLocation().add(player.getLocation().getDirection().multiply(10));
        player.getWorld().spawnParticle(Particle.PORTAL, targetLoc, 30);
        playSound(player, Sound.ENTITY_WITHER_SHOOT, 1.0f, 1.0f);

        org.bukkit.scheduler.BukkitRunnable runnable = new org.bukkit.scheduler.BukkitRunnable() {
            @Override
            public void run() {
                if (!player.isOnline()) return;
                targetLoc.getWorld().spawnParticle(Particle.LAVA, targetLoc, 100, 1, 1, 1);
                targetLoc.getWorld().spawnParticle(Particle.EXPLOSION_EMITTER, targetLoc, 5);
                playSound(player, Sound.ENTITY_GENERIC_EXPLODE, 2.0f, 0.8f);
                
                List<Entity> nearby = targetLoc.getWorld().getEntities().stream()
                    .filter(e -> e instanceof Player)
                    .filter(e -> e.getLocation().distanceSquared(targetLoc) <= 25)
                    .toList();
                
                for (Entity entity : nearby) {
                    if (entity instanceof Player p) {
                        damageIgnoreArmor(p, 12 * getPowerMultiplier(player, heart), player);
                    }
                }
            }
        };
        runnable.runTaskLater(com.aspectsmp.AspectSMP.getInstance(), 40L);

        applyCooldown(player, getId(), 25000L);
        return true;
    }
}