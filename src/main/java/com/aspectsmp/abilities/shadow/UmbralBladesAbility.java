package com.aspectsmp.abilities.shadow;

import com.aspectsmp.abilities.BaseAbility;
import com.aspectsmp.core.Heart;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.util.Vector;

import java.util.List;

public class UmbralBladesAbility extends BaseAbility {

    @Override
    public String getId() {
        return "shadow:umbral_blades";
    }

    @Override
    public String getName() {
        return "Umbral Blades";
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

        player.getWorld().spawnParticle(Particle.SOUL_FIRE_FLAME, player.getLocation().add(0, 1, 0), 30, 1, 1, 1);
        playSound(player, Sound.ENTITY_WARDEN_SONIC_CHARGE, 1.0f, 1.0f);

        Vector direction = player.getLocation().getDirection().normalize();
        for (double i = 1; i <= 8; i += 0.5) {
            org.bukkit.Location point = player.getLocation().add(direction.clone().multiply(i));
            point.getWorld().spawnParticle(Particle.SOUL, point, 3, 0.3, 0.3, 0.3);
            
            point.getWorld().getEntities().stream()
                .filter(e -> e instanceof LivingEntity && e != player && !(e instanceof org.bukkit.entity.Villager))
                .filter(e -> e.getLocation().distanceSquared(point) <= 4)
                .forEach(e -> {
                    if (e instanceof LivingEntity target) {
                        damageIgnoreArmor(target, 4.0 * getPowerMultiplier(player, heart), player);
                        target.addPotionEffect(new org.bukkit.potion.PotionEffect(
                            org.bukkit.potion.PotionEffectType.WITHER, 100, 0), true);
                    }
                });
        }

        applyCooldown(player, getId(), getCooldownDuration(heart));
        return true;
    }

    @Override
    protected long getCooldownBase() {
        return 15000L;
    }
}