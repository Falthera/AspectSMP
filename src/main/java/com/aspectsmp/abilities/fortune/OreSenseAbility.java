package com.aspectsmp.abilities.fortune;

import com.aspectsmp.abilities.BaseAbility;
import com.aspectsmp.core.Heart;
import org.bukkit.Particle;
import org.bukkit.Sound;
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
        player.sendMessage("§7Prospecting for ores...");

        int radius = 10;
        int heightRange = 5;
        int duration = 100;
        int interval = 10;

        new org.bukkit.scheduler.BukkitRunnable() {
            int ticks = 0;
            @Override
            public void run() {
                ticks++;
                org.bukkit.Location center = player.getLocation();
                for (int x = -radius; x <= radius; x++) {
                    for (int y = -heightRange; y <= heightRange; y++) {
                        for (int z = -radius; z <= radius; z++) {
                            org.bukkit.block.Block block = center.clone().add(x, y, z).getBlock();
                            if (block.getType().toString().contains("ORE")) {
                                player.getWorld().spawnParticle(Particle.DRIPPING_HONEY, block.getLocation().add(0.5, 0.5, 0.5), 2, 0.2, 0.2, 0.2);
                            }
                        }
                    }
                }
                if (ticks >= duration) cancel();
            }
        }.runTaskTimer(com.aspectsmp.AspectSMP.getInstance(), 0L, interval);

        applyCooldown(player, getId(), 60000L);
        return true;
    }
}
