package com.aspectsmp.abilities.winter;

import com.aspectsmp.abilities.BaseAbility;
import com.aspectsmp.core.Heart;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

public class AbsoluteZeroAbility extends BaseAbility {

    private static final double RADIUS = 7.5;
    private static final int FREEZE_DURATION_TICKS = 100;

    @Override
    public String getId() {
        return "winter:absolute_zero";
    }

    @Override
    public String getName() {
        return "Absolute Zero";
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

        Vector center = player.getLocation().toVector();
        for (Entity entity : player.getNearbyEntities(RADIUS, 5.0, RADIUS)) {
            if (entity instanceof LivingEntity target && target != player && !(target instanceof org.bukkit.entity.Villager)) {
                target.addPotionEffect(new org.bukkit.potion.PotionEffect(org.bukkit.potion.PotionEffectType.SLOWNESS, FREEZE_DURATION_TICKS, 4));
                target.addPotionEffect(new org.bukkit.potion.PotionEffect(org.bukkit.potion.PotionEffectType.JUMP_BOOST, FREEZE_DURATION_TICKS, 4));
                target.setFreezeTicks(FREEZE_DURATION_TICKS);
                target.getWorld().spawnParticle(Particle.CRIT, target.getLocation().add(0, 1, 0), 30, 0.4, 0.8, 0.4, 0.1);
            }
        }

        player.getWorld().spawnParticle(Particle.CLOUD, player.getLocation().add(0, 1, 0), 100, RADIUS, 2.0, RADIUS, 0.02);
        player.getWorld().spawnParticle(Particle.END_ROD, player.getLocation().add(0, 1, 0), 60, RADIUS * 0.6, 1.0, RADIUS * 0.6, 0.02);
        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_PLAYER_HURT_FREEZE, 2.0f, 0.5f);

        new BukkitRunnable() {
            @Override
            public void run() {
                player.getWorld().spawnParticle(Particle.CRIT, player.getLocation().add(0, 0.5, 0), 40, RADIUS * 0.5, 0.5, RADIUS * 0.5, 0.05);
                player.getWorld().playSound(player.getLocation(), Sound.BLOCK_GLASS_BREAK, 1.5f, 0.8f);
            }
        }.runTaskLater(com.aspectsmp.AspectSMP.getInstance(), 20L);

        applyCooldown(player, getId(), getCooldownDuration(heart));
        return true;
    }

    @Override
    protected long getCooldownBase() {
        return 120000L;
    }
}
