package com.aspectsmp.abilities.tide;

import com.aspectsmp.abilities.BaseAbility;
import com.aspectsmp.core.Heart;
import com.aspectsmp.AspectSMP;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;

public class MaelstromAbility extends BaseAbility {

    @Override
    public String getId() {
        return "tide:maelstrom";
    }

    @Override
    public String getName() {
        return "Leviathan's Blessing";
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
            player, com.aspectsmp.core.AspectType.TIDE, getId());

        Location center = player.getLocation();
        playSound(player, Sound.ENTITY_DOLPHIN_PLAY, 1.0f, 0.5f);

        // Reworked: Pull + damage + Mining Fatigue in 6 block radius for 8s (was 5s in 5 blocks)
        // Pull force: 0.5 (was 0.4)
        // Damage: 4/s (was 3)
        // Mining Fatigue 2 (was 1)
        // Radius: 6 blocks (was 5)
        // Duration: 8s (was 5s)
        // Cooldown: 1.5 min (was 1.3 min)
        // Uptime: 8s/120s = 6.67%
        // Total cost: ~48 PP (control-focused)
        
        new org.bukkit.scheduler.BukkitRunnable() {
            int ticks = 0;
            @Override
            public void run() {
                if (!player.isOnline() || ticks >= 80) {
                    cancel();
                    return;
                }
                ticks += 10;
                center.getWorld().spawnParticle(Particle.BUBBLE_COLUMN_UP, center, 30, 4, 1, 4);
                center.getWorld().getEntities().stream()
                    .filter(e -> e instanceof Player && e != player && e.getLocation().distanceSquared(center) <= 36) // 6 blocks
                    .forEach(e -> {
                        Vector pull = center.toVector().subtract(e.getLocation().toVector()).normalize().multiply(0.5);
                        e.setVelocity(pull);
                        if (e instanceof Player target) {
                            damageIgnoreArmor(target, 4.0 * getPowerMultiplier(player, heart), player);
                            target.addPotionEffect(new org.bukkit.potion.PotionEffect(
                                org.bukkit.potion.PotionEffectType.MINING_FATIGUE, 40, 1), true); // MF 2
                        }
                    });
            }
        }.runTaskTimer(AspectSMP.getInstance(), 0L, 10L);

        // Cooldown: 2 min (was 1.3 min)
        // Uptime: 8s/120s = 6.67%
        // Total cost: ~50 PP (control-focused)
        applyCooldown(player, getId(), 120000L);
        return true;
    }

    @Override
    protected long getCooldownBase() {
        return 120000L; // 2 minutes
    }
}
