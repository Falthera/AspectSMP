package com.aspectsmp.abilities.cosmos;

import com.aspectsmp.abilities.BaseAbility;
import com.aspectsmp.core.Heart;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;

import java.util.List;

public class BlackStarCollapseAbility extends BaseAbility {

    @Override
    public String getId() {
        return "cosmos:black_star_collapse";
    }

    @Override
    public String getName() {
        return "Black Star Collapse";
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

        org.bukkit.Location loc = player.getLocation();
        
        for (int i = 0; i < 10; i++) {
            org.bukkit.scheduler.BukkitRunnable runnable = new org.bukkit.scheduler.BukkitRunnable() {
                @Override
                public void run() {
                    loc.getWorld().spawnParticle(Particle.PORTAL, loc.clone().add(Math.random() - 0.5, Math.random(), Math.random() - 0.5), 30);
                    loc.getWorld().spawnParticle(Particle.END_ROD, loc.clone().add(Math.random() - 0.5, Math.random(), Math.random() - 0.5), 20);
                }
            };
            runnable.runTaskLater(com.aspectsmp.AspectSMP.getInstance(), i * 5L);
        }
        
        playSound(player, Sound.ENTITY_ENDER_DRAGON_DEATH, 2.0f, 0.5f);

        List<Entity> nearby = loc.getWorld().getEntities().stream()
            .filter(e -> e instanceof Player && e != player)
            .filter(e -> e.getLocation().distanceSquared(loc) <= 64)
            .toList();

        for (Entity entity : nearby) {
            if (entity instanceof Player target) {
                target.damage(15 * getPowerMultiplier(heart), player);
            }
        }

        applyCooldown(player, getId(), 60000L);
        return true;
    }
}