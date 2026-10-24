package com.aspectsmp.abilities.shadow;

import com.aspectsmp.AspectSMP;
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

import java.util.List;
import java.util.Arrays;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

public class VoidReaperAbility extends BaseAbility {

    @Override
    public String getId() {
        return "shadow:void_reaper";
    }

    @Override
    public String getName() {
        return "Void Reaper";
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

        org.bukkit.Location center = player.getLocation();
        player.getWorld().spawnParticle(Particle.PORTAL, center, 100, 5, 3, 5, 0.2);
        player.getWorld().spawnParticle(Particle.SOUL_FIRE_FLAME, center, 50, 4, 2, 4, 0.1);
        player.getWorld().playSound(center, Sound.ENTITY_WITHER_SPAWN, 2.0f, 0.4f);
        player.getWorld().playSound(center, Sound.ENTITY_ENDER_DRAGON_DEATH, 1.5f, 0.5f);

        // Shadow clone attacks
        new BukkitRunnable() {
            int ticks = 0;
            @Override
            public void run() {
                if (!player.isOnline() || ticks >= 200) { cancel(); return; }
                ticks += 20;
                
                for (Entity entity : center.getWorld().getNearbyEntities(center, 10, 6, 10)) {
                    if (entity instanceof LivingEntity target && target != player && !(target instanceof org.bukkit.entity.Villager)) {
                        // Shadow projectile
                        org.bukkit.util.Vector toTarget = target.getLocation().toVector().subtract(center.toVector()).normalize();
                        target.getWorld().spawnParticle(Particle.SOUL, center.clone().add(0, 1, 0), 5, 0.2, 0.2, 0.2);
                        target.getWorld().spawnParticle(Particle.SOUL, target.getLocation().add(0, 1, 0), 10, 0.3, 0.3, 0.3);
                        
                        damageIgnoreArmor(target, 5.0 * getPowerMultiplier(player, heart), player);
                        target.addPotionEffect(new org.bukkit.potion.PotionEffect(
                            org.bukkit.potion.PotionEffectType.WITHER, 80, 1), true);
                        target.addPotionEffect(new org.bukkit.potion.PotionEffect(
                            org.bukkit.potion.PotionEffectType.BLINDNESS, 60, 0), true);
                    }
                }
            }
        }.runTaskTimer(AspectSMP.getInstance(), 0L, 20L);

        // Player buff
        List<PotionEffect> effects = Arrays.asList(
            new PotionEffect(PotionEffectType.STRENGTH, 200, 1),
            new PotionEffect(PotionEffectType.SPEED, 200, 1),
            new PotionEffect(PotionEffectType.NIGHT_VISION, 200, 0)
        );
        applyTemporaryEffects(player, effects, 200L);

        applyCooldown(player, getId(), getCooldownDuration(heart));
        return true;
    }

    @Override
    protected long getCooldownBase() {
        return 180000L;
    }
}