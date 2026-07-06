package com.aspectsmp.abilities.tempest;

import com.aspectsmp.abilities.BaseAbility;
import com.aspectsmp.core.Heart;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;

import java.util.List;

public class ThunderExecutionAbility extends BaseAbility {

    @Override
    public String getId() {
        return "tempest:thunder_execution";
    }

    @Override
    public String getName() {
        return "Thunder Execution";
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
    public boolean execute(Player player, Heart heart, Event triggerEvent) {
        if (!canExecute(player, heart)) return false;
        if (isOnCooldown(player, getId())) return false;

        Entity target = player.getWorld().getEntities().stream()
            .filter(e -> e instanceof Player && e != player)
            .filter(e -> ((Player) e).getHealth() <= 10)
            .min((a, b) -> Double.compare(a.getLocation().distanceSquared(player.getLocation()),
                  b.getLocation().distanceSquared(player.getLocation())))
            .orElse(null);

        if (target instanceof Player p && p.getHealth() <= 10) {
            p.getWorld().spawnParticle(Particle.ELECTRIC_SPARK, p.getLocation().add(0, 1, 0), 100);
            p.damage(20 * getPowerMultiplier(heart), player);
            playSound(player, Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 2.0f, 0.5f);
        }

        applyCooldown(player, getId(), 45000L);
        return true;
    }
}