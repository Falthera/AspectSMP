package com.aspectsmp.abilities.inferno;

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
import org.bukkit.Location;

import java.util.List;

public class SunCoreCollapseAbility extends BaseAbility {

    @Override
    public String getId() {
        return "inferno:sun_core_collapse";
    }

    @Override
    public String getName() {
        return "Sun Core Collapse";
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
            player, com.aspectsmp.core.AspectType.INFERNO, getId());

        for (int i = 0; i < 10; i++) {
            double d = i * 0.5;
            org.bukkit.scheduler.BukkitRunnable runnable = new org.bukkit.scheduler.BukkitRunnable() {
                @Override
                public void run() {
                    player.getWorld().spawnParticle(Particle.FLAME, player.getLocation().add(0, d, 0), 80, 2, 2, 2);
                }
            };
            runnable.runTaskLater(AspectSMP.getInstance(), (long) d * 5);
        }

        playSound(player, Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 1.0f, 0.5f);

        // Balanced: 8 dmg + fire in 5 block radius (was 10 dmg in 5 blocks)
        // Cooldown: 3 min (was 1.5 min)
        // Uptime: 5s/180s = 2.78%
        // Total cost: ~48 PP (DPS-focused)
        
        Location center = player.getLocation();
        List<Entity> nearby = center.getWorld().getEntities().stream()
            .filter(e -> e != player && e.getLocation().distanceSquared(center) <= 25)
            .toList();

        for (Entity entity : nearby) {
            if (entity instanceof Player target) {
                damageIgnoreArmor(target, 8 * getPowerMultiplier(player, heart), player); // was 10
                target.setFireTicks(60); // was 80
            }
        }

        // Self-buff: Fire Resistance + Strength 1 for 10s
        player.addPotionEffect(new org.bukkit.potion.PotionEffect(
            org.bukkit.potion.PotionEffectType.FIRE_RESISTANCE, 200, 0), true);
        player.addPotionEffect(new org.bukkit.potion.PotionEffect(
            org.bukkit.potion.PotionEffectType.STRENGTH, 200, 0), true);

        // Cooldown: 3 min (was 1.5 min)
        // Uptime: 5s/180s = 2.78%
        // Total cost: ~48 PP (DPS-focused)
        applyCooldown(player, getId(), 180000L);
        return true;
    }

    @Override
    protected long getCooldownBase() {
        return 180000L; // 3 minutes
    }
}
