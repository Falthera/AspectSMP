package com.aspectsmp.abilities.winter;

import com.aspectsmp.abilities.BaseAbility;
import com.aspectsmp.core.Heart;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

public class IciclesAbility extends BaseAbility {

    private static final double RADIUS = 8.0;
    private static final int FALL_DELAY_TICKS = 10;
    private static final int FROST_DURATION_TICKS = 60;

    @Override
    public String getId() {
        return "winter:icicles";
    }

    @Override
    public String getName() {
        return "Icicles";
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

        for (Entity entity : player.getNearbyEntities(RADIUS, 5.0, RADIUS)) {
            if (entity instanceof LivingEntity target && target != player && !(target instanceof org.bukkit.entity.Villager)) {
                Location targetLoc = target.getLocation().add(0, 8, 0);
                target.getWorld().spawnParticle(Particle.CRIMSON_SPORE, targetLoc, 20, 0.5, 0.2, 0.5, 0.02);
                target.getWorld().playSound(targetLoc, Sound.BLOCK_POINTED_DRIPSTONE_LAND, 1.0f, 1.2f);

                new BukkitRunnable() {
                    @Override
                    public void run() {
                        if (target.isDead()) {
                            cancel();
                            return;
                        }
                        damageIgnoreArmor(target, 4.0 + getPowerMultiplier(player, heart), player);
                        target.addPotionEffect(new org.bukkit.potion.PotionEffect(org.bukkit.potion.PotionEffectType.SLOWNESS, FROST_DURATION_TICKS, 0));
                        target.getWorld().spawnParticle(Particle.CRIT, target.getLocation().add(0, 1, 0), 30, 0.3, 0.5, 0.3, 0.05);
                        target.getWorld().playSound(target.getLocation(), Sound.ENTITY_PLAYER_HURT_FREEZE, 1.0f, 1.0f);
                    }
                }.runTaskLater(com.aspectsmp.AspectSMP.getInstance(), FALL_DELAY_TICKS);
            }
        }

        applyCooldown(player, getId(), getCooldownDuration(heart));
        return true;
    }

    @Override
    protected long getCooldownBase() {
        return 4000L;
    }
}
