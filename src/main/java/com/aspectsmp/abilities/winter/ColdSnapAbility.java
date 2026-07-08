package com.aspectsmp.abilities.winter;

import com.aspectsmp.abilities.BaseAbility;
import com.aspectsmp.core.Heart;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.util.Vector;

public class ColdSnapAbility extends BaseAbility {

    private static final double RADIUS = 6.0;
    private static final int FREEZE_DURATION_TICKS = 60;

    @Override
    public String getId() {
        return "winter:cold_snap";
    }

    @Override
    public String getName() {
        return "Cold Snap";
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
        for (Entity entity : player.getNearbyEntities(RADIUS, 3.0, RADIUS)) {
            if (entity instanceof LivingEntity target && target != player) {
                Vector toTarget = target.getLocation().toVector().subtract(player.getLocation().toVector()).normalize();
                double dot = direction.dot(toTarget);
                if (dot > 0.3) {
                    target.addPotionEffect(new org.bukkit.potion.PotionEffect(org.bukkit.potion.PotionEffectType.SLOWNESS, FREEZE_DURATION_TICKS, 4));
                    target.addPotionEffect(new org.bukkit.potion.PotionEffect(org.bukkit.potion.PotionEffectType.JUMP_BOOST, FREEZE_DURATION_TICKS, 4));
                    target.setFreezeTicks(FREEZE_DURATION_TICKS);
                    target.getWorld().spawnParticle(Particle.ITEM_CRACK, target.getLocation().add(0, 1, 0), 40, 0.4, 0.8, 0.4, 0.1);
                    target.getWorld().playSound(target.getLocation(), Sound.ENTITY_PLAYER_HURT_FREEZE, 1.5f, 0.6f);
                }
            }
        }

        player.getWorld().spawnParticle(Particle.CLOUD, player.getLocation().add(0, 1, 0), 30, 2.0, 1.0, 2.0, 0.02);
        playSound(player, Sound.BLOCK_GLASS_BREAK, 1.0f, 0.5f);

        applyCooldown(player, getId(), getCooldownDuration(heart));
        return true;
    }

    @Override
    protected long getCooldownBase() {
        return 6000L;
    }
}
