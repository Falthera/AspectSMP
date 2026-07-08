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

public class TempestCageAbility extends BaseAbility {

    private static final double OUTER_RADIUS = 5.0;
    private static final double INNER_RADIUS = 3.0;
    private static final int DURATION_TICKS = 100;

    @Override
    public String getId() {
        return "cloud:tempest_cage";
    }

    @Override
    public String getName() {
        return "Tempest Cage";
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

        Vector center = player.getLocation().toVector();
        for (Entity entity : player.getNearbyEntities(OUTER_RADIUS, 3.0, OUTER_RADIUS)) {
            if (entity instanceof LivingEntity target && target != player) {
                Vector toCenter = center.clone().subtract(target.getLocation().toVector());
                double dist = toCenter.length();
                if (dist > INNER_RADIUS) {
                    Vector push = toCenter.normalize().multiply(0.4);
                    target.setVelocity(target.getVelocity().add(push));
                }
                target.addPotionEffect(new org.bukkit.potion.PotionEffect(org.bukkit.potion.PotionEffectType.SLOWNESS, DURATION_TICKS, 1));
            }
        }

        player.getWorld().spawnParticle(Particle.CLOUD, player.getLocation().add(0, 1, 0), 50, OUTER_RADIUS, 1.5, OUTER_RADIUS, 0.02);
        player.getWorld().spawnParticle(Particle.END_ROD, player.getLocation().add(0, 1, 0), 30, OUTER_RADIUS * 0.6, 1.0, OUTER_RADIUS * 0.6, 0.02);
        playSound(player, Sound.ENTITY_BREEZE_SLAM, 1.2f, 0.7f);

        applyCooldown(player, getId(), getCooldownDuration(heart));
        return true;
    }
}
