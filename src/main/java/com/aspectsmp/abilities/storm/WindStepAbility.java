package com.aspectsmp.abilities.storm;

import com.aspectsmp.abilities.BaseAbility;
import com.aspectsmp.core.Heart;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.util.Vector;

public class WindStepAbility extends BaseAbility {

    @Override
    public String getId() {
        return "storm:wind_step";
    }

    @Override
    public String getName() {
        return "Wind Step";
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

        Vector direction = player.getLocation().getDirection().normalize().multiply(4.0);
        direction.setY(0.5);
        player.setVelocity(direction);

        player.getWorld().spawnParticle(Particle.CLOUD, player.getLocation().add(0, 1, 0), 40, 1, 1, 1, 0.05);
        player.getWorld().spawnParticle(Particle.SWEEP_ATTACK, player.getLocation().add(0, 1, 0), 20, 0.5, 0.5, 0.5);
        playSound(player, Sound.ENTITY_BREEZE_WIND_BURST, 1.0f, 1.2f);

        // Knockback and damage enemies in path
        for (Entity entity : player.getNearbyEntities(3, 2, 3)) {
            if (entity instanceof LivingEntity target && target != player && !(target instanceof org.bukkit.entity.Villager)) {
                Vector knockback = direction.clone().normalize().multiply(1.2).add(new Vector(0, 0.3, 0));
                target.setVelocity(target.getVelocity().add(knockback));
                damageIgnoreArmor(target, 3.0 * getPowerMultiplier(player, heart), player);
            }
        }

        applyCooldown(player, getId(), getCooldownDuration(heart));
        return true;
    }

    @Override
    protected long getCooldownBase() {
        return 6000L;
    }
}