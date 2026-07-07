package com.aspectsmp.abilities.tide;

import com.aspectsmp.abilities.BaseAbility;
import com.aspectsmp.core.Heart;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;

public class TidalPrisonAbility extends BaseAbility {

    @Override
    public String getId() {
        return "tide:tidal_prison";
    }

    @Override
    public String getName() {
        return "Tidal Prison";
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

        org.bukkit.Location loc = player.getLocation();
        player.getWorld().spawnParticle(Particle.BUBBLE, loc, 50, 2, 2, 2);
        playSound(player, Sound.ENTITY_DOLPHIN_PLAY, 1.0f, 1.0f);

        player.getWorld().getEntities().stream()
            .filter(e -> e instanceof Player && e != player && e.getLocation().distanceSquared(loc) <= 16)
            .forEach(e -> {
                if (e instanceof Player target) {
                    target.damage(4 * getPowerMultiplier(player, heart), player);
                    target.addPotionEffect(new org.bukkit.potion.PotionEffect(org.bukkit.potion.PotionEffectType.LEVITATION, 40, 0));
                    target.addPotionEffect(new org.bukkit.potion.PotionEffect(org.bukkit.potion.PotionEffectType.SLOWNESS, 60, 1));
                    
                    org.bukkit.Location targetLoc = target.getLocation();
                    for (int x = -2; x <= 2; x++) {
                        for (int y = 0; y <= 3; y++) {
                            for (int z = -2; z <= 2; z++) {
                                if (x == -2 || x == 2 || z == -2 || z == 2) {
                                    org.bukkit.Location blockLoc = targetLoc.clone().add(x, y, z);
                                    if (blockLoc.getBlock().getType().isAir()) {
                                        blockLoc.getBlock().setType(org.bukkit.Material.BLUE_STAINED_GLASS);
                                        player.getWorld().spawnParticle(Particle.BUBBLE_POP, blockLoc.add(0.5, 0.5, 0.5), 5, 0, 0, 0);
                                    }
                                }
                            }
                        }
                    }
                    
                    new org.bukkit.scheduler.BukkitRunnable() {
                        @Override
                        public void run() {
                            for (int x = -2; x <= 2; x++) {
                                for (int y = 0; y <= 3; y++) {
                                    for (int z = -2; z <= 2; z++) {
                                        if (x == -2 || x == 2 || z == -2 || z == 2) {
                                            org.bukkit.Location blockLoc = targetLoc.clone().add(x, y, z);
                                            if (blockLoc.getBlock().getType() == org.bukkit.Material.BLUE_STAINED_GLASS) {
                                                blockLoc.getBlock().setType(org.bukkit.Material.AIR);
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }.runTaskLater(com.aspectsmp.AspectSMP.getInstance(), 60L);
                }
            });

        applyCooldown(player, getId(), 15000L);
        return true;
    }
}