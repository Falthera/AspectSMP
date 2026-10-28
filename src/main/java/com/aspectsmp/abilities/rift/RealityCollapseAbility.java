package com.aspectsmp.abilities.rift;

import com.aspectsmp.abilities.BaseAbility;
import com.aspectsmp.core.Heart;
import com.aspectsmp.AspectSMP;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;

import java.util.List;

public class RealityCollapseAbility extends BaseAbility {

    @Override
    public String getId() {
        return "rift:reality_collapse";
    }

    @Override
    public String getName() {
        return "Reality Collapse";
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
            player, com.aspectsmp.core.AspectType.RIFT, getId());

        Location loc = player.getLocation();

        for (int i = 0; i < 5; i++) {
            org.bukkit.scheduler.BukkitRunnable runnable = new org.bukkit.scheduler.BukkitRunnable() {
                @Override
                public void run() {
                    loc.getWorld().spawnParticle(Particle.PORTAL, loc.clone().add(Math.random() - 0.5, Math.random(), Math.random() - 0.5), 25);
                }
            };
            runnable.runTaskLater(AspectSMP.getInstance(), i * 10L);
        }

        playSound(player, Sound.BLOCK_PORTAL_TRIGGER, 2.0f, 0.5f);

        // Balanced: Teleport swap + damage in 6 block radius (was 6 blocks, 8 dmg)
        // Teleport: swap with nearest enemy in 10 blocks
        // Damage: 6 (was 8), Radius: 6 blocks
        // Cooldown: 3 min (was 1.3 min)
        // Uptime: 5s/180s = 2.78%
        // Total cost: ~48 PP (control-focused)
        
        List<Entity> nearby = loc.getWorld().getEntities().stream()
            .filter(e -> e instanceof Player && e != player && e.getLocation().distanceSquared(loc) <= 36)
            .toList();

        // Teleport swap with nearest enemy
        nearby.stream()
            .filter(e -> e instanceof Player)
            .min((a, b) -> Double.compare(a.getLocation().distanceSquared(loc), b.getLocation().distanceSquared(loc)))
            .ifPresent(e -> {
                Player target = (Player) e;
                Location playerLoc = player.getLocation().clone();
                player.teleport(target.getLocation());
                target.teleport(playerLoc);
                target.getWorld().spawnParticle(org.bukkit.Particle.PORTAL, target.getLocation(), 30, 1, 1, 1, 0.1);
            });

        // Damage + Slow in 6 block radius
        for (Entity entity : nearby) {
            if (entity instanceof Player target) {
                damageIgnoreArmor(target, 6.0 * getPowerMultiplier(player, heart), player); // was 8
                target.addPotionEffect(new org.bukkit.potion.PotionEffect(
                    org.bukkit.potion.PotionEffectType.SLOWNESS, 60, 1), true); // Slow 2 for 3s
            }
        }

        // Self-buff: Speed 2 + Resistance 1 for 10s
        player.addPotionEffect(new org.bukkit.potion.PotionEffect(
            org.bukkit.potion.PotionEffectType.SPEED, 200, 1), true);
        player.addPotionEffect(new org.bukkit.potion.PotionEffect(
            org.bukkit.potion.PotionEffectType.RESISTANCE, 200, 0), true);

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
