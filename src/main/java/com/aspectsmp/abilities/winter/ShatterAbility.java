package com.aspectsmp.abilities.winter;

import com.aspectsmp.abilities.BaseAbility;
import com.aspectsmp.core.Heart;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.potion.PotionEffect;

public class ShatterAbility extends BaseAbility {

    private static final double FULL_DAMAGE = 6.0;
    private static final double REDUCED_DAMAGE = 2.0;
    private static final double RADIUS = 5.0;

    @Override
    public String getId() {
        return "winter:shatter";
    }

    @Override
    public String getName() {
        return "Shatter";
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

        for (Entity entity : player.getNearbyEntities(RADIUS, 5.0, RADIUS)) {
            if (entity instanceof LivingEntity target && target != player && !(target instanceof org.bukkit.entity.Villager)) {
                boolean isFrozen = target.getFreezeTicks() > 0 ||
                    target.getPotionEffect(org.bukkit.potion.PotionEffectType.SLOWNESS) != null &&
                    target.getPotionEffect(org.bukkit.potion.PotionEffectType.SLOWNESS).getAmplifier() >= 4;
                double damage = isFrozen ? FULL_DAMAGE : REDUCED_DAMAGE;
                damageIgnoreArmor(target, damage + getPowerMultiplier(player, heart), player);
                target.setFreezeTicks(0);
                target.getWorld().spawnParticle(Particle.CRIT, target.getLocation().add(0, 1, 0), 30, 0.4, 0.6, 0.4, 0.1);
                target.getWorld().playSound(target.getLocation(), Sound.ENTITY_PLAYER_HURT_FREEZE, 2.0f, 1.0f);
            }
        }

        applyCooldown(player, getId(), getCooldownDuration(heart));
        return true;
    }

    @Override
    protected long getCooldownBase() {
        return 3000L;
    }
}

