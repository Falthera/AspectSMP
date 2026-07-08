package com.aspectsmp.abilities;

import com.aspectsmp.core.Heart;
import com.aspectsmp.core.StabilityState;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.block.Biome;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.World;
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
        return Math.max(3000L, getCooldownBase() * (100 - heart.getStability()) / 50);
    }

    protected long getCooldownBase() {
        return 5000L;
    }

    protected double getPowerMultiplier(Heart heart) {
        int passiveLevel = heart.getPassiveLevel();
        return 1.0 + (passiveLevel * 0.1);
    }

    protected double getPowerMultiplier(Player player, Heart heart) {
        return getPowerMultiplier(heart) * getEnvironmentMultiplier(player, heart.getAspect());
    }

    protected double getEnvironmentMultiplier(Player player, com.aspectsmp.core.AspectType aspect) {
        double multiplier = 1.0;
        Block block = player.getLocation().getBlock();
        Biome biome = block.getBiome();
        String key = biome.getKey().toString();
        World world = player.getWorld();

        switch (aspect) {
            case INFERNO -> {
                if (key.equals("minecraft:nether") || key.equals("minecraft:desert") || key.equals("minecraft:badlands") || key.equals("minecraft:eroded_badlands") || key.equals("minecraft:wooded_badlands")) {
                    multiplier += 0.2;
                }
                if (!world.hasStorm() && !world.isThundering() && block.getTemperature() > 1.0) {
                    multiplier += 0.1;
                }
                if (world.hasStorm() || world.isThundering()) {
                    multiplier -= 0.2;
                }
            }
            case TIDE -> {
                if (key.equals("minecraft:ocean") || key.equals("minecraft:deep_ocean") || key.equals("minecraft:river") || key.equals("minecraft:frozen_river") || key.equals("minecraft:warm_ocean") || key.equals("minecraft:lukewarm_ocean") || key.equals("minecraft:cold_ocean") || key.equals("minecraft:deep_frozen_ocean") || key.equals("minecraft:deep_lukewarm_ocean") || key.equals("minecraft:deep_cold_ocean") || key.equals("minecraft:deep_warm_ocean")) {
                    multiplier += 0.2;
                }
                if (block.isLiquid()) {
                    multiplier += 0.15;
                }
                if (world.hasStorm()) {
                    multiplier += 0.1;
                }
            }
            case TEMPEST -> {
                if (world.isThundering()) {
                    multiplier += 0.25;
                } else if (world.hasStorm()) {
                    multiplier += 0.1;
                } else {
                    multiplier -= 0.1;
                }
            }
            case RIFT -> {
                if (world.getTime() > 13000 && world.getTime() < 23000) {
                    multiplier += 0.2;
                }
                if (block.getLightLevel() < 5) {
                    multiplier += 0.1;
                }
            }
            case VITALITY -> {
                if (world.getTime() < 13000 || world.getTime() > 23000) {
                    // night time, no bonus
                } else {
                    multiplier += 0.1;
                }
                if (key.equals("minecraft:plains") || key.equals("minecraft:forest") || key.equals("minecraft:flower_forest") || key.equals("minecraft:sunflower_plains") || key.equals("minecraft:birch_forest") || key.equals("minecraft:old_growth_birch_forest") || key.equals("minecraft:dark_forest")) {
                    multiplier += 0.05;
                }
            }
            case WAR -> {
                // No modifiers
            }
            case COSMOS -> {
                if (player.getLocation().getY() > 100) {
                    multiplier += 0.15;
                }
                if (world.getTime() > 13000 && world.getTime() < 23000) {
                    multiplier += 0.05;
                }
            }
            case FORTUNE -> {
                if (key.equals("minecraft:badlands") || key.equals("minecraft:eroded_badlands") || key.equals("minecraft:wooded_badlands") || key.equals("minecraft:windswept_hills") || key.equals("minecraft:windswept_gravelly_hills") || key.equals("minecraft:windswept_forest")) {
                    multiplier += 0.2;
                }
                if (block.getTemperature() < 0.5) {
                    multiplier += 0.05;
                }
            }
            case CLOUD -> {
                if (player.getLocation().getY() > 100) {
                    multiplier += 0.2;
                }
                if (world.hasStorm() || world.isThundering()) {
                    multiplier += 0.15;
                }
            }
            case WINTER -> {
                if (world.hasStorm()) {
                    multiplier += 0.2;
                }
                if (block.getTemperature() < 0.1) {
                    multiplier += 0.15;
                }
            }
        }

        return Math.max(0.5, Math.min(1.5, multiplier));
    }

    protected void generateEssence(Player player, int amount) {
        Heart heart = com.aspectsmp.AspectSMP.getInstance().getHeartManager().getHeart(player.getUniqueId()).orElse(null);
        if (heart != null) {
            heart.addEssence(amount);
        }
    }
}