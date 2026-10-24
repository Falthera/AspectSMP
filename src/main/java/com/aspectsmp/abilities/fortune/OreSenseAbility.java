package com.aspectsmp.abilities.fortune;

import com.aspectsmp.abilities.BaseAbility;
import com.aspectsmp.core.Heart;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.BlockDisplay;
import org.bukkit.entity.Player;
import org.bukkit.entity.Entity;
import org.bukkit.Location;
import org.bukkit.event.Event;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.Map;
import java.util.WeakHashMap;

public class OreSenseAbility extends BaseAbility {

    private static final Set<Material> ORE_TYPES = new HashSet<>();
    private static final Map<UUID, Set<BlockDisplay>> activeDisplays = new WeakHashMap<>();

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

        playSound(player, org.bukkit.Sound.BLOCK_AMETHYST_BLOCK_CHIME, 1.0f, 1.0f);
        player.sendMessage("§7Prospecting for ores... §f(15 seconds)");

        int radius = 15;
        int heightRange = 8;
        int duration = 300;
        int interval = 10;

        UUID playerUUID = player.getUniqueId();

        player.spawnParticle(org.bukkit.Particle.ENCHANTED_HIT, player.getLocation().add(0, 1, 0), 30, 0.5, 0.5, 0.5);

        new BukkitRunnable() {
            int ticks = 0;
            Location center = player.getLocation();
            Set<BlockDisplay> displays = new HashSet<>();

            @Override
            public void run() {
                if (!player.isOnline()) {
                    cleanupDisplays(displays);
                    activeDisplays.remove(playerUUID);
                    cancel();
                    return;
                }

                cleanupDisplays(displays);
                displays.clear();

                ticks++;
                Location currentCenter = center.clone();

                for (int x = -radius; x <= radius; x += 2) {
                    for (int y = -heightRange; y <= heightRange; y += 2) {
                        for (int z = -radius; z <= radius; z += 2) {
                            Block block = currentCenter.clone().add(x, y, z).getBlock();
                            Material type = block.getType();
                            if (ORE_TYPES.contains(type)) {
                                BlockData blockData = block.getBlockData();
                                Location displayLoc = block.getLocation().add(0.5, 0.5, 0.5);

                                                BlockDisplay display = player.getWorld().spawn(displayLoc, BlockDisplay.class);
                                display.setBlock(blockData);
                                display.setGlowing(true);
                                display.setInvulnerable(true);
                                display.setVisibleByDefault(false);
                                display.setGravity(0.0f);
                                display.setPersistent(true);
                                display.setRemoveWhenFarAway(false);

                                player.showEntity(com.aspectsmp.AspectSMP.getInstance(), display);

                                displays.add(display);
                            }
                        }
                    }
                }

                if (ticks >= duration / interval) {
                    cleanupDisplays(displays);
                    activeDisplays.remove(playerUUID);
                    cancel();
                }
            }
        }.runTaskTimer(com.aspectsmp.AspectSMP.getInstance(), 0L, interval);

        activeDisplays.put(playerUUID, new HashSet<>());

        applyCooldown(player, getId(), getCooldownDuration(heart));
        return true;
    }

    private void cleanupDisplays(Set<BlockDisplay> displays) {
        if (displays == null) return;
        for (BlockDisplay display : displays) {
            if (display != null && display.isValid()) {
                display.remove();
            }
        }
        displays.clear();
    }

    @Override
    protected long getCooldownBase() {
        return 30000L;
    }
}
