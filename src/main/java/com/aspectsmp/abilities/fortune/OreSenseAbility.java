package com.aspectsmp.abilities.fortune;

import com.aspectsmp.abilities.BaseAbility;
import com.aspectsmp.core.Heart;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;

public class OreSenseAbility extends BaseAbility {

    @Override
    public String getId() {
        return "fortune:ore_sense";
    }

    @Override
    public String getName() {
        return "Prospector";
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

        player.getWorld().spawnParticle(Particle.ENCHANTED_HIT, player.getLocation().add(0, 1, 0), 30, 0.5, 0.5, 0.5);
        playSound(player, Sound.BLOCK_AMETHYST_BLOCK_CHIME, 1.0f, 1.0f);
        player.sendMessage("§7Prospecting for ores... §f(15 seconds)");

        int radius = 15;
        int heightRange = 8;
        int duration = 150;
        int interval = 5;

        new org.bukkit.scheduler.BukkitRunnable() {
            int ticks = 0;
            @Override
            public void run() {
                ticks++;
                Location center = player.getLocation();
                for (int x = -radius; x <= radius; x++) {
                    for (int y = -heightRange; y <= heightRange; y++) {
                        for (int z = -radius; z <= radius; z++) {
                            Block block = center.clone().add(x, y, z).getBlock();
                            Material type = block.getType();
                            Particle.DustOptions dustOptions = getOreParticleOptions(type);
                            if (dustOptions != null) {
                                player.getWorld().spawnParticle(Particle.REDSTONE, block.getLocation().add(0.5, 0.5, 0.5), 2, 0.1, 0.1, 0.1, dustOptions);
                            }
                        }
                    }
                }
                if (ticks >= duration) cancel();
            }
        }.runTaskTimer(com.aspectsmp.AspectSMP.getInstance(), 0L, interval);

        applyCooldown(player, getId(), getCooldownDuration(heart));
        return true;
    }

    @Override
    protected long getCooldownBase() {
        return 30000L;
    }

    private Particle.DustOptions getOreParticleOptions(Material type) {
        return switch (type) {
            case COAL_ORE, DEEPSLATE_COAL_ORE -> new Particle.DustOptions(org.bukkit.Color.fromRGB(0x333333), 1.0f);
            case IRON_ORE, DEEPSLATE_IRON_ORE -> new Particle.DustOptions(org.bukkit.Color.fromRGB(0xC0C0C0), 1.0f);
            case GOLD_ORE, DEEPSLATE_GOLD_ORE, NETHER_GOLD_ORE -> new Particle.DustOptions(org.bukkit.Color.fromRGB(0xFFD700), 1.0f);
            case REDSTONE_ORE, DEEPSLATE_REDSTONE_ORE, NETHER_REDSTONE_ORE -> new Particle.DustOptions(org.bukkit.Color.fromRGB(0xFF0000), 1.0f);
            case LAPIS_ORE, DEEPSLATE_LAPIS_ORE -> new Particle.DustOptions(org.bukkit.Color.fromRGB(0x0000FF), 1.0f);
            case EMERALD_ORE, DEEPSLATE_EMERALD_ORE -> new Particle.DustOptions(org.bukkit.Color.fromRGB(0x00FF00), 1.0f);
            case DIAMOND_ORE, DEEPSLATE_DIAMOND_ORE -> new Particle.DustOptions(org.bukkit.Color.fromRGB(0x00FFFF), 1.0f);
            case ANCIENT_DEBRIS -> new Particle.DustOptions(org.bukkit.Color.fromRGB(0x800080), 1.0f);
            case NETHER_QUARTZ_ORE -> new Particle.DustOptions(org.bukkit.Color.fromRGB(0xFFFFFF), 1.0f);
            default -> null;
        };
    }
}
