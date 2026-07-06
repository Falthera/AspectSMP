package com.aspectsmp.abilities.inferno;

import com.aspectsmp.abilities.BaseAbility;
import com.aspectsmp.core.Heart;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.util.Vector;

public class FlameStepAbility extends BaseAbility {

    @Override
    public String getId() {
        return "inferno:flame_step";
    }

    @Override
    public String getName() {
        return "Flame Step";
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

        Vector direction = player.getLocation().getDirection().multiply(0.8);
        player.setVelocity(player.getVelocity().add(direction));

        World world = player.getWorld();
        Location start = player.getLocation();
        for (int i = 0; i < 8; i++) {
            Location forward = start.clone().add(direction.clone().multiply(0.5 * (i + 1)));
            world.spawnParticle(Particle.FLAME, forward, 5, 0.2, 0.2, 0.2, 0.05);
        }

        spawnParticles(player, Particle.FLAME, 30, 0.5, 0.5, 0.5);
        playSound(player, Sound.ENTITY_BLAZE_SHOOT, 1.0f, 1.0f);

        for (Entity entity : player.getNearbyEntities(4.0, 2.0, 4.0)) {
            if (entity instanceof LivingEntity target && target != player) {
                target.damage(3.0 + getPowerMultiplier(heart), player);
                target.setFireTicks(40);
            }
        }

        applyCooldown(player, getId(), getCooldownDuration(heart));
        return true;
    }

    @Override
    protected long getCooldownBase() {
        return 80000L;
    }
}