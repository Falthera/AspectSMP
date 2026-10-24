package com.aspectsmp.abilities.fortune;

import com.aspectsmp.abilities.BaseAbility;
import com.aspectsmp.core.Heart;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.HashSet;
import java.util.Set;

public class OreSenseAbility extends BaseAbility {

    private static final Set<Material> ORE_TYPES = new HashSet<>();

    static {
        ORE_TYPES.add(Material.COAL_ORE);
        ORE_TYPES.add(Material.DEEPSLATE_COAL_ORE);
        ORE_TYPES.add(Material.IRON_ORE);
        ORE_TYPES.add(Material.DEEPSLATE_IRON_ORE);
        ORE_TYPES.add(Material.GOLD_ORE);
        ORE_TYPES.add(Material.DEEPSLATE_GOLD_ORE);
        ORE_TYPES.add(Material.REDSTONE_ORE);
        ORE_TYPES.add(Material.DEEPSLATE_REDSTONE_ORE);
        ORE_TYPES.add(Material.LAPIS_ORE);
        ORE_TYPES.add(Material.DEEPSLATE_LAPIS_ORE);
        ORE_TYPES.add(Material.EMERALD_ORE);
        ORE_TYPES.add(Material.DEEPSLATE_EMERALD_ORE);
        ORE_TYPES.add(Material.DIAMOND_ORE);
        ORE_TYPES.add(Material.DEEPSLATE_DIAMOND_ORE);
        ORE_TYPES.add(Material.ANCIENT_DEBRIS);
        ORE_TYPES.add(Material.NETHER_GOLD_ORE);
        ORE_TYPES.add(Material.NETHER_QUARTZ_ORE);
        ORE_TYPES.add(Material.NETHER_REDSTONE_ORE);
    }

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
        int interval = 10;

        new BukkitRunnable() {
            int ticks = 0;
            Location center = player.getLocation();

            @Override
            public void run() {
                if (!player.isOnline()) {
                    cancel();
                    return;
                }
                ticks++;

                Location currentCenter = center.clone();
                for (int x = -radius; x <= radius; x += 2) {
                    for (int y = -heightRange; y <= heightRange; y += 2) {
                        for (int z = -radius; z <= radius; z += 2) {
                            Block block = currentCenter.clone().add(x, y, z).getBlock();
                            Material type = block.getType();
                            if (ORE_TYPES.contains(type)) {
                                BlockData blockData = block.getBlockData();
                                Location particleLoc = block.getLocation().add(0.5, 0.5, 0.5);

                                player.spawnParticle(Particle.BLOCK_DUST, particleLoc, 3, 0.1, 0.1, 0.1, 0, blockData, true);

                                Particle.DustOptions dustOptions = getOreParticleOptions(type);
                                if (dustOptions != null) {
                                    player.spawnParticle(Particle.REDSTONE, particleLoc, 1, 0, 0, 0, 0, dustOptions, true);
                                }
                            }
                        }
                    }
                }

                if (ticks >= duration / interval) cancel();
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
