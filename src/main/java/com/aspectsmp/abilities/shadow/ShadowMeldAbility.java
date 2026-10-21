package com.aspectsmp.abilities.shadow;

import com.aspectsmp.abilities.BaseAbility;
import com.aspectsmp.core.Heart;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;

public class ShadowMeldAbility extends BaseAbility {

    @Override
    public String getId() {
        return "shadow:shadow_meld";
    }

    @Override
    public String getName() {
        return "Shadow Meld";
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

        player.getWorld().spawnParticle(Particle.SMOKE, player.getLocation().add(0, 1, 0), 80, 2, 2, 2, 0.05);
        player.getWorld().spawnParticle(Particle.SOUL, player.getLocation().add(0, 1, 0), 50, 1.5, 1.5, 1.5, 0.03);
        playSound(player, Sound.ENTITY_ENDERMAN_TELEPORT, 0.8f, 0.5f);

        // True invisibility + intangibility for 8 seconds
        player.addPotionEffect(new org.bukkit.potion.PotionEffect(
            org.bukkit.potion.PotionEffectType.INVISIBILITY, 160, 0, true, false), true);
        player.addPotionEffect(new org.bukkit.potion.PotionEffect(
            org.bukkit.potion.PotionEffectType.DAMAGE_RESISTANCE, 160, 4, true, false), true);
        player.addPotionEffect(new org.bukkit.potion.PotionEffect(
            org.bukkit.potion.PotionEffectType.SPEED, 160, 1, true, false), true);

        // Damage nearby enemies when emerging
        new org.bukkit.scheduler.BukkitRunnable() {
            @Override
            public void run() {
                if (!player.isOnline()) return;
                for (Entity entity : player.getNearbyEntities(4, 3, 4)) {
                    if (entity instanceof LivingEntity target && target != player && !(target instanceof org.bukkit.entity.Villager)) {
                        damageIgnoreArmor(target, 6.0 * getPowerMultiplier(player, heart), player);
                        target.getWorld().spawnParticle(Particle.SOUL_FIRE_FLAME, target.getLocation().add(0, 1, 0), 20, 0.5, 0.5, 0.5);
                    }
                }
            }
        }.runTaskLater(plugin, 160L);

        applyCooldown(player, getId(), getCooldownDuration(heart));
        return true;
    }

    @Override
    protected long getCooldownBase() {
        return 30000L;
    }
}