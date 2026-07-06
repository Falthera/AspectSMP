package com.aspectsmp.abilities;

import com.aspectsmp.core.Heart;
import com.aspectsmp.core.StabilityState;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public abstract class BaseAbility implements Ability {

    protected static final Map<UUID, Long> COOLDOWNS = new HashMap<>();

    protected void applyCooldown(Player player, String abilityId, long durationMillis) {
        Heart heart = com.aspectsmp.AspectSMP.getInstance().getHeartManager().getHeart(player.getUniqueId()).orElse(null);
        if (heart != null) {
            heart.getCooldowns().put(abilityId, System.currentTimeMillis() + durationMillis);
        }
    }

    protected boolean isOnCooldown(Player player, String abilityId) {
        Heart heart = com.aspectsmp.AspectSMP.getInstance().getHeartManager().getHeart(player.getUniqueId()).orElse(null);
        if (heart == null) return true;
        return heart.getCooldowns().getOrDefault(abilityId, 0L) > System.currentTimeMillis();
    }

    protected void playSound(Player player, Sound sound, float volume, float pitch) {
        player.getWorld().playSound(player.getLocation(), sound, volume, pitch);
    }

    protected void spawnParticles(Player player, Particle particle, int count, double offsetX, double offsetY, double offsetZ) {
        player.getWorld().spawnParticle(particle, player.getLocation().add(0, 1, 0), count, offsetX, offsetY, offsetZ);
    }

    protected long getCooldownDuration(Heart heart) {
        return getCooldownBase() * (100 - heart.getStability()) / 50;
    }

    protected long getCooldownBase() {
        return 5000L;
    }

    protected double getPowerMultiplier(Heart heart) {
        int passiveLevel = heart.getPassiveLevel();
        return 1.0 + (passiveLevel * 0.1);
    }

    protected void generateEssence(Player player, int amount) {
        Heart heart = com.aspectsmp.AspectSMP.getInstance().getHeartManager().getHeart(player.getUniqueId()).orElse(null);
        if (heart != null) {
            heart.addEssence(amount);
        }
    }
}