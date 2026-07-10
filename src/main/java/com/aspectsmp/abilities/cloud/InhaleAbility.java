package com.aspectsmp.abilities.cloud;

import com.aspectsmp.abilities.BaseAbility;
import com.aspectsmp.core.Heart;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.util.Vector;

public class InhaleAbility extends BaseAbility {

    private static final double PULL_RADIUS = 6.0;
    private static final double PULL_FORCE = 0.8;

    @Override
    public String getId() {
        return "cloud:inhale";
    }

    @Override
    public String getName() {
        return "Inhale";
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

        Vector toCenter = player.getLocation().toVector();
        for (Entity entity : player.getNearbyEntities(PULL_RADIUS, PULL_RADIUS, PULL_RADIUS)) {
            if (entity instanceof LivingEntity target && target != player && !(target instanceof org.bukkit.entity.Villager)) {
                Vector pull = toCenter.clone().subtract(target.getLocation().toVector()).normalize().multiply(PULL_FORCE);
                target.setVelocity(pull);
                target.getWorld().spawnParticle(Particle.CLOUD, target.getLocation().add(0, 1, 0), 10, 0.3, 0.5, 0.3, 0.01);
            }
        }

        player.getWorld().spawnParticle(Particle.END_ROD, player.getLocation().add(0, 1, 0), 30, 1.5, 1.0, 1.5, 0.02);
        playSound(player, Sound.ENTITY_BREEZE_IDLE_GROUND, 1.5f, 0.8f);

        applyCooldown(player, getId(), getCooldownDuration(heart));
        return true;
    }
}
