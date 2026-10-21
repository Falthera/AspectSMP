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

        Vector direction = player.getLocation().getDirection().normalize().multiply(3.0);
        player.setVelocity(direction.setY(0.3));

        World world = player.getWorld();
        Location start = player.getLocation();
        for (int i = 0; i < 6; i++) {
            Location forward = start.clone().add(direction.clone().multiply(0.5 * (i + 1)));
            world.spawnParticle(Particle.FLAME, forward, 3, 0.15, 0.15, 0.15, 0.03);
        }

        spawnParticles(player, Particle.FLAME, 20, 0.4, 0.4, 0.4);
        playSound(player, Sound.ENTITY_BLAZE_SHOOT, 1.0f, 1.2f);

        for (Entity entity : player.getNearbyEntities(3.0, 2.0, 3.0)) {
            if (entity instanceof LivingEntity target && target != player && !(target instanceof org.bukkit.entity.Villager)) {
                damageIgnoreArmor(target, 2.5 + getPowerMultiplier(player, heart), player);
                target.setFireTicks(30);
            }
        }

        // Play impact visuals for affected entities
        java.util.Collection<LivingEntity> targets = player.getNearbyEntities(3.0, 2.0, 3.0).stream()
            .filter(e -> e instanceof LivingEntity && e != player && !(e instanceof org.bukkit.entity.Villager))
            .map(e -> (LivingEntity) e)
            .collect(java.util.stream.Collectors.toList());
        if (!targets.isEmpty()) {
            playImpactVisuals(player, heart.getAspect(), player.getLocation(), 1.0, targets);
        }

        applyCooldown(player, getId(), getCooldownDuration(heart));
        return true;
    }

    @Override
    protected long getCooldownBase() {
        return 5000L;
    }
}