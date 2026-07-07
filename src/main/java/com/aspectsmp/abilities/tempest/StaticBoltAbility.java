package com.aspectsmp.abilities.tempest;

import com.aspectsmp.abilities.BaseAbility;
import com.aspectsmp.core.Heart;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;

import java.util.List;

public class StaticBoltAbility extends BaseAbility {

    @Override
    public String getId() {
        return "tempest:static_bolt";
    }

    @Override
    public String getName() {
        return "Thunder Strike";
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

        Entity target = player.getWorld().getEntities().stream()
            .filter(e -> e instanceof Player && e != player)
            .min((a, b) -> Double.compare(a.getLocation().distanceSquared(player.getLocation()),
                  b.getLocation().distanceSquared(player.getLocation())))
            .orElse(null);

        if (target instanceof Player p) {
            p.getWorld().spawnParticle(Particle.ELECTRIC_SPARK, p.getLocation().add(0, 1, 0), 50);
            p.damage(8 * getPowerMultiplier(player, heart), player);
            playSound(player, Sound.ENTITY_LIGHTNING_BOLT_IMPACT, 1.0f, 1.0f);
        }

        applyCooldown(player, getId(), 10000L);
        return true;
    }
}