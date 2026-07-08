package com.aspectsmp.abilities.cloud;

import com.aspectsmp.abilities.BaseAbility;
import com.aspectsmp.core.Heart;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

public class CloudburstAbility extends BaseAbility {

    private static final double LAUNCH_FORCE = 1.8;
    private static final double DAMAGE_RADIUS = 6.0;
    private static final int SLAM_DELAY_TICKS = 40;

    @Override
    public String getId() {
        return "cloud:cloudburst";
    }

    @Override
    public String getName() {
        return "Cloudburst";
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
    public boolean isUltimate() {
        return true;
    }

    @Override
    public boolean execute(Player player, Heart heart, Event triggerEvent) {
        if (!canExecute(player, heart)) return false;
        if (isOnCooldown(player, getId())) return false;

        Vector center = player.getLocation().toVector();
        for (Entity entity : player.getNearbyEntities(DAMAGE_RADIUS, 5.0, DAMAGE_RADIUS)) {
            if (entity instanceof LivingEntity target && target != player) {
                Vector launch = new Vector(0, LAUNCH_FORCE, 0);
                target.setVelocity(launch);
                target.getWorld().spawnParticle(Particle.CLOUD, target.getLocation().add(0, 1, 0), 15, 0.3, 0.5, 0.3, 0.03);
            }
        }

        player.getWorld().spawnParticle(Particle.EXPLOSION_EMITTER, player.getLocation().add(0, 1, 0), 10, DAMAGE_RADIUS * 0.5, 1.0, DAMAGE_RADIUS * 0.5, 0.1);
        playSound(player, Sound.ENTITY_BREEZE_WIND_BURST, 2.0f, 0.6f);

        new BukkitRunnable() {
            @Override
            public void run() {
                for (Entity entity : player.getNearbyEntities(DAMAGE_RADIUS, 5.0, DAMAGE_RADIUS)) {
                    if (entity instanceof LivingEntity target && target != player) {
                        Vector slam = new Vector(0, -1.2, 0);
                        target.setVelocity(slam);
                        target.damage(8.0 + getPowerMultiplier(player, heart), player);
                        target.getWorld().spawnParticle(Particle.CRIT, target.getLocation().add(0, 1, 0), 20, 0.4, 0.6, 0.4, 0.1);
                    }
                }
                player.getWorld().spawnParticle(Particle.LARGE_SMOKE, player.getLocation().add(0, 0.5, 0), 40, DAMAGE_RADIUS * 0.5, 0.5, DAMAGE_RADIUS * 0.5, 0.05);
                playSound(player, Sound.ENTITY_GENERIC_EXPLODE, 1.5f, 0.8f);
            }
        }.runTaskLater(com.aspectsmp.AspectSMP.getInstance(), SLAM_DELAY_TICKS);

        applyCooldown(player, getId(), getCooldownDuration(heart));
        return true;
    }

    @Override
    protected long getCooldownBase() {
        return 90000L;
    }
}
