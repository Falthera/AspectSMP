package com.aspectsmp.abilities.vitality;

import com.aspectsmp.abilities.BaseAbility;
import com.aspectsmp.core.Heart;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;

import java.util.List;

public class HealPulseAbility extends BaseAbility {

    @Override
    public String getId() {
        return "vitality:heal_pulse";
    }

    @Override
    public String getName() {
        return "Heal Pulse";
    }

    @Override
    public int getTier() {
        return 1;
    }

    @Override
    public boolean isPassive() {
        return false;
    }

    @Override
    public boolean execute(Player player, Heart heart, Event triggerEvent) {
        if (!canExecute(player, heart)) return false;
        if (isOnCooldown(player, getId())) return false;

        player.getWorld().spawnParticle(Particle.HEART, player.getLocation().add(0, 1, 0), 30, 2, 2, 2);
        playSound(player, Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.0f);

        List<Entity> nearby = player.getWorld().getEntities().stream()
            .filter(e -> e instanceof Player)
            .filter(e -> e.getLocation().distanceSquared(player.getLocation()) <= 16)
            .toList();

        for (Entity entity : nearby) {
            if (entity instanceof Player target) {
                if (target == player || com.aspectsmp.AspectSMP.getInstance().getTrustManager().isTrusted(player.getUniqueId(), target.getUniqueId()) || com.aspectsmp.AspectSMP.getInstance().getTrustManager().isTrusted(target.getUniqueId(), player.getUniqueId())) {
                    target.heal(6 * getPowerMultiplier(heart));
                }
            }
        }

        applyCooldown(player, getId(), 8000L);
        return true;
    }
}