package com.aspectsmp.abilities.vitality;

import com.aspectsmp.abilities.BaseAbility;
import com.aspectsmp.core.Heart;
import com.aspectsmp.AspectSMP;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.Location;

public class GenesisFieldAbility extends BaseAbility {

    @Override
    public String getId() {
        return "vitality:genesis_field";
    }

    @Override
    public String getName() {
        return "Genesis Field";
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

        com.aspectsmp.AspectSMP.getInstance().getVisualEffectsManager().playUltimateActivationVisuals(
            player, com.aspectsmp.core.AspectType.VITALITY, getId());

        org.bukkit.Location loc = player.getLocation();
        player.getWorld().spawnParticle(Particle.HEART, loc.add(0, 1, 0), 30, 2, 2, 2);
        playSound(player, Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.0f);

        new org.bukkit.scheduler.BukkitRunnable() {
            int ticks = 0;
            @Override
            public void run() {
                if (!player.isOnline() || ticks >= 40) {
                    cancel();
                    return;
                }
                ticks += 20;
                loc.getWorld().spawnParticle(Particle.HEART, loc.clone().add(Math.random() - 0.5, 0.5, Math.random() - 0.5), 10);
                for (Entity entity : loc.getWorld().getEntities()) {
                    if (entity instanceof Player target && entity.getLocation().distanceSquared(loc) <= 64) {
                        if (target == player || com.aspectsmp.AspectSMP.getInstance().getTrustManager().isTrusted(player.getUniqueId(), target.getUniqueId()) || com.aspectsmp.AspectSMP.getInstance().getTrustManager().isTrusted(target.getUniqueId(), player.getUniqueId())) {
                            target.heal(4.0 * getPowerMultiplier(player, heart));
                        }
                    }
                }
            }
        }.runTaskTimer(com.aspectsmp.AspectSMP.getInstance(), 0L, 20L);

        player.addPotionEffect(new org.bukkit.potion.PotionEffect(org.bukkit.potion.PotionEffectType.REGENERATION, 300, 1), true);
        player.addPotionEffect(new org.bukkit.potion.PotionEffect(org.bukkit.potion.PotionEffectType.RESISTANCE, 300, 0), true);

        applyCooldown(player, getId(), 120000L);
        return true;
    }

    @Override
    protected long getCooldownBase() {
        return 120000L;
    }
}