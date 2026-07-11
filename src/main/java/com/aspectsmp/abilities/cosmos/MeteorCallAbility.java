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

public class MeteorCallAbility extends BaseAbility {

    private static final double TARGET_RANGE = 30.0;
    private static final int METEOR_DELAY_TICKS = 40;
    private static final double DAMAGE_RADIUS = 5.0;
    private static final double BASE_DAMAGE = 14.0;

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
    public boolean isUltimate() {
        return true;
    }

    @Override
    public boolean execute(Player player, Heart heart, Event triggerEvent) {
        if (!canExecute(player, heart)) return false;
        if (isOnCooldown(player, getId())) return false;

        Player target = findTarget(player);
        if (target == null) {
            player.sendMessage("§cNo target found within range!");
            return false;
        }

        Vector direction = target.getLocation().toVector().subtract(player.getLocation().toVector()).normalize();
        org.bukkit.Location startLoc = player.getLocation().clone().add(direction.multiply(3)).add(0, 25, 0);
        org.bukkit.Location targetLoc = target.getLocation().clone().add(0, 15, 0);

        player.getWorld().spawnParticle(Particle.END_ROD, startLoc, 20, 1.0, 0.5, 1.0, 0.02);
        player.getWorld().spawnParticle(Particle.PORTAL, startLoc, 30, 0.5, 0.5, 0.5, 0.01);
        playSound(player, Sound.ENTITY_WITHER_SHOOT, 1.0f, 0.6f);
        player.sendMessage("§d§lMeteor incoming! Tracking " + target.getName() + "...");

        org.bukkit.scheduler.BukkitRunnable trackingTask = new org.bukkit.scheduler.BukkitRunnable() {
            int tick = 0;
            @Override
            public void run() {
                if (!player.isOnline() || target.isDead() || !target.isOnline()) {
                    this.cancel();
                    return;
                }

                targetLoc = target.getLocation().clone().add(0, 15, 0);
                Vector toTarget = targetLoc.toVector().subtract(startLoc.toVector()).normalize();
                startLoc.add(toTarget.multiply(1.5));

                startLoc.getWorld().spawnParticle(Particle.FLAME, startLoc, 3, 0.3, 0.3, 0.3, 0.02);
                startLoc.getWorld().spawnParticle(Particle.SMOKE, startLoc, 2, 0.2, 0.2, 0.2, 0.01);

                tick++;
                if (tick >= METEOR_DELAY_TICKS) {
                    this.cancel();
                    landMeteor(player, heart, targetLoc, target);
                }
            }
        };
        trackingTask.runTaskTimer(com.aspectsmp.AspectSMP.getInstance(), 0L, 1L);

        applyCooldown(player, getId(), 25000L);
        return true;
    }

    private Player findTarget(Player player) {
        Player nearest = null;
        double nearestDist = TARGET_RANGE * TARGET_RANGE;

        for (Entity entity : player.getWorld().getNearbyEntities(player.getLocation(), TARGET_RANGE, TARGET_RANGE, TARGET_RANGE)) {
            if (entity instanceof Player target && target != player && !target.isDead()) {
                if (hasLineOfSight(player, target)) {
                    double dist = target.getLocation().distanceSquared(player.getLocation());
                    if (dist < nearestDist) {
                        nearestDist = dist;
                        nearest = target;
                    }
                }
            }
        }
        return nearest;
    }

    private boolean hasLineOfSight(Player player, Player target) {
        Vector dir = target.getLocation().toVector().subtract(player.getLocation().toVector()).normalize();
        Vector playerDir = player.getLocation().getDirection();
        double dot = dir.dot(playerDir);
        return dot > 0.3;
    }

    private void landMeteor(Player player, Heart heart, org.bukkit.Location targetLoc, Player trackedTarget) {
        targetLoc.getWorld().spawnParticle(Particle.EXPLOSION_EMITTER, targetLoc, 20, 2.0, 2.0, 2.0, 0.1);
        targetLoc.getWorld().spawnParticle(Particle.LAVA, targetLoc, 40, 1.5, 1.5, 1.5, 0.05);
        targetLoc.getWorld().spawnParticle(Particle.FLAME, targetLoc, 60, DAMAGE_RADIUS, 1.0, DAMAGE_RADIUS, 0.1);
        targetLoc.getWorld().playSound(targetLoc, Sound.ENTITY_GENERIC_EXPLODE, 2.0f, 0.7f);
        targetLoc.getWorld().playSound(targetLoc, Sound.BLOCK_LAVA_EXTINGUISH, 1.0f, 0.5f);

        List<Entity> nearby = targetLoc.getWorld().getEntities().stream()
            .filter(e -> e instanceof Player)
            .filter(e -> e.getLocation().distanceSquared(targetLoc) <= DAMAGE_RADIUS * DAMAGE_RADIUS)
            .toList();

        double damage = BASE_DAMAGE + (getPowerMultiplier(heart) * 3.0);
        for (Entity entity : nearby) {
            if (entity instanceof Player p && p != player) {
                damageIgnoreArmor(p, damage, player);
                p.addPotionEffect(new org.bukkit.potion.PotionEffect(org.bukkit.potion.PotionEffectType.SLOWNESS, 60, 1));
                p.getWorld().spawnParticle(Particle.CRIT, p.getLocation().add(0, 1, 0), 20, 0.4, 0.6, 0.4, 0.1);
            }
        }

        if (trackedTarget != null && trackedTarget.isOnline() && !trackedTarget.isDead()) {
            double trackedDist = trackedTarget.getLocation().distanceSquared(targetLoc);
            if (trackedDist <= DAMAGE_RADIUS * DAMAGE_RADIUS) {
                player.sendMessage("§6§lDirect hit on " + trackedTarget.getName() + "!");
            }
        }
    }

    @Override
    protected long getCooldownBase() {
        return 25000L;
    }
}
