package com.aspectsmp.abilities.cosmos;

import com.aspectsmp.abilities.BaseAbility;
import com.aspectsmp.core.Heart;
import com.aspectsmp.AspectSMP;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;

import java.util.List;

public class BlackStarCollapseAbility extends BaseAbility {

    @Override
    public String getId() {
        return "cosmos:black_star_collapse";
    }

    @Override
    public String getName() {
        return "Black Star Collapse";
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

        AspectSMP.getInstance().getVisualEffectsManager().playUltimateActivationVisuals(
            player, com.aspectsmp.core.AspectType.COSMOS, getId());

        org.bukkit.Location loc = player.getLocation();
        
        // Visuals
        for (int i = 0; i < 10; i++) {
            org.bukkit.scheduler.BukkitRunnable runnable = new org.bukkit.scheduler.BukkitRunnable() {
                @Override
                public void run() {
                    loc.getWorld().spawnParticle(Particle.PORTAL, loc.clone().add(Math.random() - 0.5, Math.random(), Math.random() - 0.5), 25);
                    loc.getWorld().spawnParticle(Particle.END_ROD, loc.clone().add(Math.random() - 0.5, Math.random(), Math.random() - 0.5), 15);
                }
            };
            runnable.runTaskLater(AspectSMP.getInstance(), i * 5L);
        }
        
        playSound(player, Sound.ENTITY_ENDER_DRAGON_DEATH, 2.0f, 0.5f);

        // Balanced: Pull + damage in 8 block radius (was 8 blocks, 10 dmg)
        // Pull: 0.5 force, Damage: 8 (was 10), Radius: 8 blocks
        // Cooldown: 3 min (was 1.3 min)
        // Uptime: 5s/180s = 2.78%
        // Total cost: ~48 PP (control-focused)
        
        List<Entity> nearby = loc.getWorld().getEntities().stream()
            .filter(e -> e instanceof Player && e != player)
            .filter(e -> e.getLocation().distanceSquared(loc) <= 64) // 8 blocks
            .toList();

        for (Entity entity : nearby) {
            if (entity instanceof Player target) {
                // Pull toward center
                Vector toCenter = loc.toVector().subtract(target.getLocation().toVector()).normalize().multiply(0.5);
                target.setVelocity(toCenter);
                
                damageIgnoreArmor(target, 8.0 * getPowerMultiplier(player, heart), player);
                
                // Visual feedback
                target.getWorld().spawnParticle(org.bukkit.Particle.PORTAL, target.getLocation().add(0, 1, 0), 20, 0.5, 1, 0.5, 0.1);
            }
        }

        // Slow effect on all pulled targets
        for (Entity entity : nearby) {
            if (entity instanceof Player target) {
                target.addPotionEffect(new org.bukkit.potion.PotionEffect(
                    org.bukkit.potion.PotionEffectType.SLOWNESS, 60, 1), true);
            }
        }

        // Cooldown: 3 min (was 1.3 min)
        // Uptime: 5s/180s = 2.78%
        // Total cost: ~48 PP (control-focused)
        applyCooldown(player, getId(), 180000L);
        return true;
    }

    @Override
    protected long getCooldownBase() {
        return 180000L; // 3 minutes
    }
}