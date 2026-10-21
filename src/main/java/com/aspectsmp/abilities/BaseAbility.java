package com.aspectsmp.abilities;

import com.aspectsmp.AspectSMP;
import com.aspectsmp.core.Heart;
import com.aspectsmp.core.StabilityState;
import com.aspectsmp.core.AspectType;
import com.aspectsmp.visual.VisualEffectsManager;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.block.Biome;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.World;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.Collection;
import java.util.Map;
import java.util.UUID;

public abstract class BaseAbility implements Ability {

    protected void damageIgnoreArmor(LivingEntity target, double amount, Entity source) {
        target.damage(amount, source);
    }

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

    /**
     * Play ability impact visuals at the target location.
     * Call this when the ability hits targets.
     */
    protected void playImpactVisuals(Player caster, AspectType aspect, Location location, double intensity, Collection<LivingEntity> targets) {
        VisualEffectsManager vfx = AspectSMP.getInstance().getVisualEffectsManager();
        if (vfx != null) {
            vfx.playAbilityImpactVisuals(location, aspect, intensity, targets);
        }
    }

    /**
     * Play ability impact visuals at the target location (single target).
     */
    protected void playImpactVisuals(Player caster, AspectType aspect, Location location, double intensity, LivingEntity target) {
        playImpactVisuals(caster, aspect, location, intensity, java.util.Collections.singleton(target));
    }

    protected long getCooldownDuration(Heart heart) {
        return Math.max(2000L, (long) (getCooldownBase() * (1.0 - heart.getStability() / 200.0)));
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
                if (key.contains("nether") || key.contains("desert") || key.contains("badlands")) {
                    multiplier += 0.15;
                }
                if (!world.hasStorm() && !world.isThundering() && block.getTemperature() > 1.0) {
                    multiplier += 0.05;
                }
                if (world.hasStorm() || world.isThundering()) {
                    multiplier -= 0.15;
                }
            }
            case TIDE -> {
                if (key.contains("ocean") || key.contains("river")) {
                    multiplier += 0.15;
                }
                if (block.isLiquid()) {
                    multiplier += 0.1;
                }
                if (world.hasStorm()) {
                    multiplier += 0.1;
                }
            }
            case TEMPEST -> {
                if (world.isThundering()) {
                    multiplier += 0.2;
                } else if (world.hasStorm()) {
                    multiplier += 0.1;
                } else {
                    multiplier -= 0.05;
                }
            }
            case RIFT -> {
                if (world.getTime() > 13000 && world.getTime() < 23000) {
                    multiplier += 0.15;
                }
                if (block.getLightLevel() < 5) {
                    multiplier += 0.1;
                }
            }
            case VITALITY -> {
                if (world.getTime() >= 13000 && world.getTime() <= 23000) {
                    multiplier += 0.1;
                }
                if (key.contains("plains") || key.contains("forest") || key.contains("flower")) {
                    multiplier += 0.05;
                }
            }
            case WAR -> {
            }
            case COSMOS -> {
                if (player.getLocation().getY() > 100) {
                    multiplier += 0.1;
                }
                if (world.getTime() > 13000 && world.getTime() < 23000) {
                    multiplier += 0.05;
                }
            }
            case FORTUNE -> {
                if (key.contains("badlands") || key.contains("windswept")) {
                    multiplier += 0.15;
                }
                if (block.getTemperature() < 0.5) {
                    multiplier += 0.05;
                }
            }
            case CLOUD -> {
                if (player.getLocation().getY() > 100) {
                    multiplier += 0.15;
                }
                if (world.hasStorm() || world.isThundering()) {
                    multiplier += 0.1;
                }
            }
            case WINTER -> {
                if (world.hasStorm()) {
                    multiplier += 0.15;
                }
                if (block.getTemperature() < 0.1) {
                    multiplier += 0.1;
                }
            }
            case SHADOW -> {
                if (block.getLightLevel() < 5) {
                    multiplier += 0.2;
                }
                if (world.getTime() > 13000 && world.getTime() < 23000) {
                    multiplier += 0.15;
                }
            }
            case STORM -> {
                if (world.hasStorm() || world.isThundering()) {
                    multiplier += 0.25;
                }
                if (player.getLocation().getY() > 100) {
                    multiplier += 0.15;
                }
            }
        }

        return Math.max(0.6, Math.min(1.4, multiplier));
    }

    protected void generateEssence(Player player, int amount) {
        Heart heart = com.aspectsmp.AspectSMP.getInstance().getHeartManager().getHeart(player.getUniqueId()).orElse(null);
        if (heart != null) {
            heart.addEssence(amount);
        }
    }
}