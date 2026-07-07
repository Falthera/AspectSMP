package com.aspectsmp.abilities.vitality;

import com.aspectsmp.abilities.BaseAbility;
import com.aspectsmp.core.Heart;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;

import java.util.List;

public class GenesisFieldAbility extends BaseAbility {

    @Override
    public String getId() {
        return "vitality:genesis_field";
    }

    @Override
    public String getName() {
        return "Genesis Field";
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
        player.getWorld().spawnParticle(Particle.HEART, loc.add(0, 1, 0), 30, 2, 2, 2);
        playSound(player, Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.0f);

        new org.bukkit.scheduler.BukkitRunnable() {
            int ticks = 0;
            @Override
            public void run() {
                if (!player.isOnline() || ticks >= 100) {
                    cancel();
                    return;
                }
                ticks += 20;
                loc.getWorld().spawnParticle(Particle.HEART, loc.clone().add(Math.random() - 0.5, 0.5, Math.random() - 0.5), 15);
                for (Entity entity : loc.getWorld().getEntities()) {
                    if (entity instanceof Player target && entity.getLocation().distanceSquared(loc) <= 36) {
                        if (target == player || com.aspectsmp.AspectSMP.getInstance().getTrustManager().isTrusted(player.getUniqueId(), target.getUniqueId()) || com.aspectsmp.AspectSMP.getInstance().getTrustManager().isTrusted(target.getUniqueId(), player.getUniqueId())) {
                            target.heal(2 * getPowerMultiplier(player, heart));
                        }
                    }
                }
            }
        }.runTaskTimer(com.aspectsmp.AspectSMP.getInstance(), 0L, 20L);

        applyCooldown(player, getId(), 60000L);
        return true;
    }
}