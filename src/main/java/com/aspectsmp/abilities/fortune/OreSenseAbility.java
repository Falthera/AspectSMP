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
        return "Ore Sense";
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

        player.getWorld().spawnParticle(Particle.VIBRATION, player.getLocation().add(0, 1, 0), 50, 20, 20, 20);
        playSound(player, Sound.BLOCK_AMETHYST_BLOCK_CHIME, 1.0f, 1.0f);

        org.bukkit.scheduler.BukkitRunnable runnable = new org.bukkit.scheduler.BukkitRunnable() {
            int ticks = 0;
            @Override
            public void run() {
                ticks++;
                org.bukkit.Location center = player.getLocation();
                for (int x = -20; x <= 20; x++) {
                    for (int y = -10; y <= 10; y++) {
                        for (int z = -20; z <= 20; z++) {
                            org.bukkit.block.Block block = center.clone().add(x, y, z).getBlock();
                            if (block.getType().toString().contains("ORE")) {
                                player.getWorld().spawnParticle(Particle.HAPPY_HONEY, block.getLocation().add(0.5, 0.5, 0.5), 1);
                            }
                        }
                    }
                }
                if (ticks >= 200) cancel();
            }
        };
        runnable.runTaskTimer(com.aspectsmp.AspectSMP.getInstance(), 0L, 5L);

        applyCooldown(player, getId(), 60000L);
        return true;
    }
}