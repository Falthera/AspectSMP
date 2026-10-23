package com.aspectsmp.abilities.tempest;

import com.aspectsmp.abilities.BaseAbility;
import com.aspectsmp.core.Heart;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.util.Vector;

import java.util.Collection;

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
        Vector direction = player.getLocation().getDirection().multiply(1.8);
        player.setVelocity(direction);

        player.getWorld().spawnParticle(Particle.ELECTRIC_SPARK, player.getLocation(), 80, 0.5, 0.5, 0.5);
        playSound(player, Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 1.0f, 1.0f);

        org.bukkit.scheduler.BukkitRunnable runnable = new org.bukkit.scheduler.BukkitRunnable() {
            @Override
            public void run() {
                if (player.isOnline()) {
                    player.setInvulnerable(false);
                    // Deal damage to nearby enemies at end of rush
                    Collection<Entity> nearby = player.getWorld().getNearbyEntities(player.getLocation(), 3, 2, 3);
                    for (Entity entity : nearby) {
                        if (entity instanceof LivingEntity target && target != player && !(target instanceof org.bukkit.entity.Villager)) {
                            damageIgnoreArmor(target, 6.0 * getPowerMultiplier(player, heart), player);
                            target.getWorld().spawnParticle(org.bukkit.Particle.ELECTRIC_SPARK, target.getLocation(), 20, 0.5, 1, 0.5, 0.1);
                        }
                    }
                }
            }
        };
        runnable.runTaskLater(com.aspectsmp.AspectSMP.getInstance(), 30L);

        applyCooldown(player, getId(), getCooldownDuration(heart));
        return true;
    }

    @Override
    protected long getCooldownBase() {
        return 18000L;
    }
}
