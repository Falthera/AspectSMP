package com.aspectsmp.abilities.tempest;

import com.aspectsmp.abilities.BaseAbility;
import com.aspectsmp.core.Heart;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;

import java.util.Comparator;
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
    public boolean isUltimate() {
        return true;
    }

    @Override
    public boolean execute(Player player, Heart heart, Event triggerEvent) {
        if (!canExecute(player, heart)) return false;
        if (isOnCooldown(player, getId())) return false;

        List<Player> nearby = player.getWorld().getPlayers().stream()
            .filter(e -> e != player && e.getLocation().distanceSquared(player.getLocation()) <= 100)
            .filter(e -> e.getHealth() <= 10)
            .min(Comparator.comparingDouble(e -> e.getLocation().distanceSquared(player.getLocation())))
            .stream().toList();

        for (Player target : nearby) {
            target.damage(20 * getPowerMultiplier(heart), player);
            player.getWorld().spawnParticle(Particle.ELECTRIC_SPARK, target.getLocation().add(0, 1, 0), 100);
            playSound(player, Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 2.0f, 0.5f);
        }

        applyCooldown(player, getId(), 45000L);
        return true;
    }
}
