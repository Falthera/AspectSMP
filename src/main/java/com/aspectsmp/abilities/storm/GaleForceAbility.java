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

import java.util.List;

public class GaleForceAbility extends BaseAbility {

    @Override
    public String getId() {
        return "storm:gale_force";
    }

    @Override
    public String getName() {
        return "Gale Force";
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

        Vector direction = player.getLocation().getDirection().normalize();
        player.getWorld().spawnParticle(Particle.CLOUD, player.getLocation().add(0, 1, 0), 60, 1, 1, 1, 0.1);
        playSound(player, Sound.ENTITY_BREEZE_WIND_BURST, 1.5f, 0.8f);

        for (double i = 0.5; i <= 12; i += 0.5) {
            org.bukkit.Location point = player.getLocation().add(direction.clone().multiply(i));
            point.getWorld().spawnParticle(Particle.SWEEP_ATTACK, point, 5, 0.5, 0.5, 0.5);
            
            point.getWorld().getEntities().stream()
                .filter(e -> e instanceof LivingEntity && e != player && !(e instanceof org.bukkit.entity.Villager))
                .filter(e -> e.getLocation().distanceSquared(point) <= 9)
                .forEach(e -> {
                    if (e instanceof LivingEntity target) {
                        Vector push = direction.clone().multiply(1.5).add(new Vector(0, 0.5, 0));
                        target.setVelocity(target.getVelocity().add(push));
                        damageIgnoreArmor(target, 3.5 * getPowerMultiplier(player, heart), player);
                        target.addPotionEffect(new org.bukkit.potion.PotionEffect(
                            org.bukkit.potion.PotionEffectType.SLOWNESS, 60, 1), true);
                    }
                });
        }

        applyCooldown(player, getId(), getCooldownDuration(heart));
        return true;
    }

    @Override
    protected long getCooldownBase() {
        return 12000L;
    }
}