package com.aspectsmp.abilities.tempest;

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

import java.util.Comparator;
import java.util.List;

public class ThunderExecutionAbility extends BaseAbility {

    @Override
    public String getId() {
        return "tempest:thunder_execution";
    }

    @Override
    public String getName() {
        return "Thunder Execution";
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
            player, com.aspectsmp.core.AspectType.TEMPEST, getId());

        // Reworked: No longer execute. Instead, chain lightning to up to 3 targets within 15 blocks
        // Damage: 8 per hit (was 12 execute)
        // Cooldown: 3 min (was 1 min - was 5x too fast)
        // Uptime: 5s/180s = 2.78%
        
        AspectSMP.getInstance().getVisualEffectsManager().playUltimateActivationVisuals(
            player, com.aspectsmp.core.AspectType.TEMPEST, getId());

        Location center = player.getLocation();
        
        // Find up to 3 targets within 15 blocks
        List<Player> targets = player.getWorld().getPlayers().stream()
            .filter(e -> e != player && e.getLocation().distanceSquared(player.getLocation()) <= 225) // 15^2
            .sorted(Comparator.comparingDouble(e -> e.getLocation().distanceSquared(player.getLocation())))
            .limit(3)
            .toList();

        if (targets.isEmpty()) {
            player.sendMessage("§cNo valid targets within 15 blocks!");
            // Still put on cooldown but reduced
            applyCooldown(player, getId(), 60000L); // 1 min penalty for miss
            return true;
        }

        for (Player target : targets) {
            // Chain lightning visual
            player.getWorld().spawnParticle(org.bukkit.Particle.ELECTRIC_SPARK, target.getLocation().add(0, 1, 0), 80);
            playSound(target, Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 2.0f, 0.5f);
            
            // Damage: 8 per hit (was 12 execute)
            damageIgnoreArmor(target, 8.0 * getPowerMultiplier(player, heart), player);
            
            // Stun for 1s + Slowness
target.addPotionEffect(new org.bukkit.potion.PotionEffect(
    org.bukkit.potion.PotionEffectType.SLOWNESS, 20, 2), true); // was STUN
            target.addPotionEffect(new org.bukkit.potion.PotionEffect(
                org.bukkit.potion.PotionEffectType.SLOWNESS, 60, 1), true);
        }

        // Self-buff: Speed 2 for 10s (chase potential)
        // Power cost: 10s duration / 180s cd = 5.5% uptime
        player.addPotionEffect(new org.bukkit.potion.PotionEffect(
            org.bukkit.potion.PotionEffectType.SPEED, 200, 1), true); // Speed 2

        // Cooldown: 3 min (was 1 min)
        // Uptime: 5s/180s = 2.78%
        // Total cost: ~48 PP
        applyCooldown(player, getId(), 180000L);
        return true;
    }

    @Override
    protected long getCooldownBase() {
        return 180000L; // 3 minutes
    }
}
