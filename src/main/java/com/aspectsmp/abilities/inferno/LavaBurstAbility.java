package com.aspectsmp.abilities.inferno;

import com.aspectsmp.abilities.BaseAbility;
import com.aspectsmp.core.Heart;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;

import java.util.List;

public class LavaBurstAbility extends BaseAbility {

    @Override
    public String getId() {
        return "inferno:lava_burst";
    }

    @Override
    public String getName() {
        return "Lava Burst";
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

        player.getWorld().spawnParticle(Particle.LAVA, player.getLocation().add(0, 1, 0), 50, 1, 1, 1);
        playSound(player, Sound.ENTITY_BLAZE_DEATH, 1.0f, 0.5f);

        List<Entity> nearby = player.getWorld().getEntities().stream()
            .filter(e -> e.getLocation().distanceSquared(player.getLocation()) <= 9)
            .toList();

        for (Entity entity : nearby) {
            if (entity instanceof Player target && target != player) {
                target.damage(6 * getPowerMultiplier(heart), player);
                target.setFireTicks(target.getFireTicks() + 60);
            }
        }

        applyCooldown(player, getId(), 12000L);
        return true;
    }
}
