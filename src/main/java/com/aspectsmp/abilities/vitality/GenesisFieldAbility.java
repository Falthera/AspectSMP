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
        
        for (int i = 0; i < 5; i++) {
            org.bukkit.scheduler.BukkitRunnable runnable = new org.bukkit.scheduler.BukkitRunnable() {
                @Override
                public void run() {
                    loc.getWorld().spawnParticle(Particle.HEART, loc.clone().add(Math.random() - 0.5, 0.5, Math.random() - 0.5), 20);
                }
            };
            runnable.runTaskLater(com.aspectsmp.AspectSMP.getInstance(), i * 20L);
        }
        
        playSound(player, Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.0f);

        List<Entity> nearby = loc.getWorld().getEntities().stream()
            .filter(e -> e instanceof Player)
            .filter(e -> e.getLocation().distanceSquared(loc) <= 36)
            .toList();

        for (Entity entity : nearby) {
            if (entity instanceof Player target) {
                if (target == player || com.aspectsmp.AspectSMP.getInstance().getTrustManager().isTrusted(player.getUniqueId(), target.getUniqueId()) || com.aspectsmp.AspectSMP.getInstance().getTrustManager().isTrusted(target.getUniqueId(), player.getUniqueId())) {
                    target.heal(10 * getPowerMultiplier(heart));
                }
            }
        }

        applyCooldown(player, getId(), 60000L);
        return true;
    }
}